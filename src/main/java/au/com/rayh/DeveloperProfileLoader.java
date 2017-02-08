package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Installs {@link DeveloperProfile} into the current slave and unlocks its keychain
 * in preparation for the signing that uses it.
 *
 * TODO: destroy identity in the end.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeveloperProfileLoader extends Builder {
    private final String id;

    @DataBoundConstructor
    public DeveloperProfileLoader(String profileId) {
        this.id = profileId;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        DeveloperProfile dp = getProfile(build.getProject());
        if (dp==null)
            throw new AbortException("No Apple developer profile is configured");

		// macOS Sierra (10.12) added extra steps needed for the keychain functions and thus code signing, namely set-key-partition-list
		// here we'll check to see if the OS version of the node running the build is >= 10.12
		// if it is, then we need to perform this extra step to avoid issues with code signing
		boolean setKeyPartitionList = (osVersionCheck(Computer.currentComputer().getSystemProperties().get("os.version").toString(), "10.11") > 0);

        // Note: keychain are usualy suffixed with .keychain. If we change we should probably clean up the ones we created
        String keyChain = "jenkins-"+build.getProject().getFullName().replace('/', '-');
        String keychainPass = UUID.randomUUID().toString();

        ArgumentListBuilder args;

        {// if the key chain is already present, delete it and start fresh
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            args = new ArgumentListBuilder("security","delete-keychain", keyChain);
            launcher.launch().cmds(args).stdout(out).join();
        }


        args = new ArgumentListBuilder("security","create-keychain");
        args.add("-p").addMasked(keychainPass);
        args.add(keyChain);
        invoke(launcher, listener, args, "Failed to create a keychain");

        args = new ArgumentListBuilder("security","unlock-keychain");
        args.add("-p").addMasked(keychainPass);
        args.add(keyChain);
        invoke(launcher, listener, args, "Failed to unlock keychain");


		// add keychains to search path
        args = new ArgumentListBuilder("security","list-keychains");
        args.add("-d", "user");
        args.add("-s", keyChain, "login.keychain", "System.keychain");
        invoke(launcher, listener, args, "Failed to set keychain search paths");
        
		// set default keychain to our active one
        args = new ArgumentListBuilder("security","default-keychain");
        args.add("-s", keyChain);
        invoke(launcher, listener, args, "Failed to set default keychain");

		// set timeout to 20 minutes to be safe
        args = new ArgumentListBuilder("security","set-keychain-settings");		
        args.add("-t", "1200");
        invoke(launcher, listener, args, "Failed to set keychain timeout");


        final FilePath secret = getSecretDir(build, keychainPass);
        secret.unzipFrom(new ByteArrayInputStream(dp.getImage()));

        // import identities
        for (FilePath id : secret.list("**/*.p12")) {
            args = new ArgumentListBuilder("security","import");
            args.add(id).add("-k",keyChain);
            args.add("-P").addMasked(dp.getPassword().getPlainText());
            
            // flag is different depending on OS
            if (setKeyPartitionList) {
	            args.add("-A");
	        } else {
	            args.add("-T","/usr/bin/codesign");
	            args.add("-T","/usr/bin/productsign");
	        }
	        
            args.add(keyChain);
            invoke(launcher, listener, args, "Failed to import identity "+id);
        }


		// necessary for functional code signing on macOS Sierra (10.12) and up
        if (setKeyPartitionList) {		
			args = new ArgumentListBuilder("security","set-key-partition-list");		
			args.add("-S", "apple-tool:,apple:");
			args.add("-s");
			args.add("-k").addMasked(keychainPass);
			args.add(keyChain);
			invoke(launcher, listener, args, "Failed to set permissions for keychain");
		}

        {
            // display keychain info for potential troubleshooting
            args = new ArgumentListBuilder("security","show-keychain-info");
            args.add(keyChain);
            ByteArrayOutputStream output = invoke(launcher, listener, args, "Failed to show keychain info");
            listener.getLogger().write(output.toByteArray());
        }

        // copy provisioning profiles
        VirtualChannel ch = build.getBuiltOn().getChannel();
        FilePath home = ch.call(new GetHomeDirectory());    // TODO: switch to FilePath.getHomeDirectory(ch) when we can
        FilePath profiles = home.child("Library/MobileDevice/Provisioning Profiles");
        profiles.mkdirs();

        for (FilePath mp : secret.list("**/*.mobileprovision")) {
            listener.getLogger().println("Installing  "+mp.getName());
            mp.copyTo(profiles.child(mp.getName()));
        }

        return true;
    }

    private ByteArrayOutputStream invoke(Launcher launcher, BuildListener listener, ArgumentListBuilder args, String errorMessage) throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (launcher.launch().cmds(args).stdout(output).join()!=0) {
            listener.getLogger().write(output.toByteArray());
            throw new AbortException(errorMessage);
        }
        return output;
    }

    private FilePath getSecretDir(AbstractBuild<?, ?> build, String keychainPass) throws IOException, InterruptedException {
        FilePath secrets = build.getBuiltOn().getRootPath().child("developer-profiles");
        secrets.mkdirs();
        secrets.chmod(0700);
        return secrets.child(keychainPass);
    }

    public DeveloperProfile getProfile(Item context) {
        List<DeveloperProfile> profiles = CredentialsProvider
                .lookupCredentials(DeveloperProfile.class, context, Jenkins.getAuthentication());
        for (DeveloperProfile c : profiles) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        // if there's no match, just go with something in the hope that it'll do
        return !profiles.isEmpty() ? profiles.get(0) : null;
    }

    public String getProfileId() {
        return id;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Import developer profile";
        }

        public ListBoxModel doFillProfileIdItems(@AncestorInPath Item context) {
            List<DeveloperProfile> profiles = CredentialsProvider
                    .lookupCredentials(DeveloperProfile.class, context, null);
            ListBoxModel r = new ListBoxModel();
            for (DeveloperProfile p : profiles) {
                r.add(p.getDescription(), p.getId());
            }
            return r;
        }
    }

    private static final class GetHomeDirectory implements Callable<FilePath,IOException> {
        public FilePath call() throws IOException {
            return new FilePath(new File(System.getProperty("user.home")));
        }
    }
    
	private static int osVersionCheck(String str1, String str2) {
	    String[] vals1 = str1.split("\\.");
	    String[] vals2 = str2.split("\\.");
	    int i = 0;
	    
	    // set index to first non-equal ordinal or length of shortest version string
	    while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
	    	i++;
	    }
	    
	    // compare first non-equal ordinal number
	    if (i < vals1.length && i < vals2.length) {
	    	int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
	        return Integer.signum(diff);
	    }
	    
	    // the strings are equal or one string is a substring of the other
	    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
	    return Integer.signum(vals1.length - vals2.length);
	}
}
