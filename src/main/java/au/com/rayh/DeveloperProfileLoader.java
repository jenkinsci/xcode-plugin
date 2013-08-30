package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
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

        String keyChain = "jenkins-"+build.getProject().getFullName().replace('/', '-');
        String keychainPass = UUID.randomUUID().toString();

        ArgumentListBuilder args;

        {// if the key chain is already present, delete it and start fresh
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            args = new ArgumentListBuilder("security","list-keychains");
            if (launcher.launch().cmds(args).stdout(out).join()!=0) {
                listener.getLogger().write(out.toByteArray());
                throw new AbortException("Failed to list keychains");
            }

            if (out.toString().contains("/"+keyChain+"\"")) {  // TODO: encoding
                args = new ArgumentListBuilder("security","delete-keychain",keyChain);
                invoke(launcher, listener, args, "Failed to delete the keychain");
            }
        }


        args = new ArgumentListBuilder("security","create-keychain");
        args.add("-p").addMasked(keychainPass);
        args.add(keyChain);
        invoke(launcher, listener, args, "Failed to create a keychain");

        args = new ArgumentListBuilder("security","unlock-keychain");
        args.add("-p").addMasked(keychainPass);
        args.add(keyChain);
        invoke(launcher, listener, args, "Failed to unlock keychain");

        final FilePath secret = getSecretDir(build, keychainPass);
        secret.unzipFrom(new ByteArrayInputStream(dp.getImage()));

        // import identities
        for (FilePath id : secret.list("**/*.p12")) {
            args = new ArgumentListBuilder("security","import");
            args.add(id).add("-k",keyChain);
            args.add("-P").addMasked(dp.getPassword().getPlainText());
            args.add("-T","/usr/bin/codesign");
            args.add(keyChain);
            invoke(launcher, listener, args, "Failed to import identity "+id);
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

    private void invoke(Launcher launcher, BuildListener listener, ArgumentListBuilder args, String errorMessage) throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (launcher.launch().cmds(args).stdout(output).join()!=0) {
            listener.getLogger().write(output.toByteArray());
            throw new AbortException(errorMessage);
        }
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
}
