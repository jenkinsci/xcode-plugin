package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;

/**
 * Installs {@link DeveloperProfile} into the current slave and unlocks its keychain
 * in preparation for the signing that uses it.
 *
 * TODO: destroy identity in the end.
 *
 * @author Kohsuke Kawaguchi
 */
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class DeveloperProfileLoader extends Builder implements SimpleBuildStep {
    private final String id;
    private boolean isProjectScoped;
    private String keychainPassword;
    private String keychainName;
    private String provisioningpProfileName;
    private boolean defaultKeychain;

    @DataBoundConstructor
    public DeveloperProfileLoader(String profileId) {
        this.id = profileId;
    }

    public void setProjectScope(boolean value) {
        this.isProjectScoped = value;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        _perform(build, build.getWorkspace(), launcher, listener);
        return true;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath filePath, Launcher launcher, TaskListener taskListener) throws InterruptedException, IOException {
        _perform(build, filePath, launcher, taskListener);
    }

    public String getKeychainPassword() {
        return keychainPassword;
    }

    public void setKeychainPassword(String keychainPassword) {
        this.keychainPassword = keychainPassword;
    }

    public String getKeychainName() {
        return keychainName;
    }

    public boolean isDefaultKeychain() {
        return defaultKeychain;
    }

    public void setDefaultKeychain(boolean defaultKeychain) {
        this.defaultKeychain = defaultKeychain;
    }

    public void setKeychainName(String keychainName) {
        this.keychainName = keychainName;
    }

    private DeveloperProfile buildProfile(Run<?, ?> build, boolean hasContext) {
        AbstractBuild ab;

        if (hasContext) {
            ab = (AbstractBuild)build;
            return getProfile(ab.getProject());
        }

        return getProfile();
    }

    private DeveloperProfile buildProfile(Run<?, ?> build) {
        return buildProfile(build, this.isProjectScoped);
    }

    public void _perform(@Nonnull Run<?, ?> build, FilePath filePath, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        DeveloperProfile dp = buildProfile(build);

        if (dp==null)
            throw new AbortException("No Apple developer profile is configured");

        // Note: keychain are usualy suffixed with .keychain. If we change we should probably clean up the ones we created
        String keyChain = "jenkins-"+build.getFullDisplayName().replace('/', '-');
        this.keychainName = keyChain;
        String keychainPass;

        if (this.keychainPassword != null) {
            keychainPass = this.keychainPassword;
        } else {
            keychainPass = UUID.randomUUID().toString();
        }

        this.keychainPassword = keychainPass;

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

        if (this.defaultKeychain) {
            args = new ArgumentListBuilder("security", "default-keychain");
            args.add("-d").add("user");
            args.add(keyChain);
            invoke(launcher, listener, args, "Failed to set default keychain");
        }

        final FilePath secret = getSecretDir(filePath, keychainPass);

        try {
            secret.unzipFrom(new ByteArrayInputStream(dp.getImage()));
        } catch (NullPointerException e) {
            throw new AbortException("Unable to read developer profile file.");
        }

        // import identities
        for (FilePath id : secret.list("**/*.p12")) {

            args = new ArgumentListBuilder("security","import");
            args.add(id).add("-k",keyChain);
            args.add("-P").addMasked(dp.getPassword().getPlainText());
            args.add("-T","/usr/bin/codesign");
            args.add("-T","/usr/bin/productsign");
            args.add(keyChain);
            invoke(launcher, listener, args, "Failed to import identity "+id);
        }

        {
            // display keychain info for potential troubleshooting
            args = new ArgumentListBuilder("security","show-keychain-info");
            args.add(keyChain);
            ByteArrayOutputStream output = invoke(launcher, listener, args, "Failed to show keychain info");
            listener.getLogger().write(output.toByteArray());
        }

        // copy provisioning profiles
        VirtualChannel ch = filePath.getChannel();
        FilePath home = filePath.getHomeDirectory(ch);
        FilePath profiles = home.child("Library/MobileDevice/Provisioning Profiles");
        profiles.mkdirs();

        for (FilePath mp : secret.list("**/*.mobileprovision")) {
            this.provisioningpProfileName = mp.getName();
            listener.getLogger().println("Installing  " + mp.getName());
            if (profiles.child(this.provisioningpProfileName).exists()) {
                String uuid = UUID.randomUUID().toString();
                this.provisioningpProfileName = uuid + ".mobileprovision";
            }
            mp.copyTo(profiles.child(this.provisioningpProfileName));
        }

        if (!this.isProjectScoped) {
            this.setPerms(launcher, listener);
            this.enableTempKeychain(launcher, listener, filePath);
            this.importAppleCert(launcher, listener, filePath);
        }
    }

    private ByteArrayOutputStream invoke(Launcher launcher, TaskListener listener, ArgumentListBuilder args, String errorMessage) throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (launcher.launch().cmds(args).stdout(output).join()!=0) {
            listener.getLogger().write(output.toByteArray());
            throw new AbortException(errorMessage);
        }
        return output;
    }

    protected FilePath getSecretDir(FilePath path,  String keychainPass) throws IOException, InterruptedException {
        FilePath secrets = path.child("developer-profiles");
        secrets.mkdirs();
        secrets.chmod(0700);
        return secrets.child(keychainPass);
    }

    public FilePath getSecretDir(FilePath path) throws IOException, InterruptedException {
        return this.getSecretDir(path, this.keychainPassword);
    }

    public DeveloperProfile getProfile() {
        List<DeveloperProfile> profiles = CredentialsProvider
                .lookupCredentials(DeveloperProfile.class, Jenkins.getAuthentication());
        for (DeveloperProfile c : profiles) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        // if there's no match, just go with something in the hope that it'll do
        return !profiles.isEmpty() ? profiles.get(0) : null;
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

    public void unload(FilePath filePath, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder("security", "delete-keychain", this.keychainName);
        ByteArrayOutputStream output = invoke(launcher, listener, args, "Failed to remove keychain");
        listener.getLogger().write(output.toByteArray());
        filePath
                .getHomeDirectory(filePath.getChannel())
                .child("Library/MobileDevice/Provisioning Profiles")
                .child(this.provisioningpProfileName).delete();
        FilePath profilePath = this.getSecretDir(filePath, this.keychainPassword);
        profilePath.deleteRecursive();
    }

    public void setPerms(Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        launcher
                .launch()
                .cmds("security", "set-key-partition-list", "-S", "apple-tool:,apple:,codesign:", "-k", this.keychainPassword, this.keychainName)
                .stdout(out)
                .join();
        listener.getLogger().write(out.toByteArray());
    }

    public void enableTempKeychain(Launcher launcher, TaskListener listener, FilePath workspace) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        FilePath homeFolder = workspace.getHomeDirectory(workspace.getChannel());
        String homePath = homeFolder.getRemote();

        launcher
                .launch()
                .cmds("security", "list-keychain", "-d", "user", "-s", homePath + "/Library/Keychains/" + this.keychainName)
                .stdout(out)
                .stderr(err)
                .join();
        if (err.size() > 0) {
            listener.getLogger().write(err.toByteArray());
        } else {
            listener.getLogger().write(out.toByteArray());
        }
    }

    public void importAppleCert(Launcher launcher, TaskListener listener, FilePath workspace) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FilePath homeFolder = workspace.getHomeDirectory(workspace.getChannel());
        String homePath = homeFolder.getRemote();

        String cert = homePath + "/AppleWWDRCA.cer";
        launcher
                .launch()
                .cmds("security", "import", cert, "-k", this.keychainName)
                .stdout(out)
                .join();
        listener.getLogger().write(out.toByteArray());
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

    private static final class GetHomeDirectory extends MasterToSlaveCallable<FilePath,IOException> {
        public FilePath call() throws IOException {
            return new FilePath(new File(System.getProperty("user.home")));
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }
    }
}
