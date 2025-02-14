package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.security.MasterToSlaveCallable;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class DeveloperProfileLoader extends Builder implements SimpleBuildStep {
    @CheckForNull
    private String profileId;
    @CheckForNull
    private Boolean importIntoExistingKeychain;
    @CheckForNull
    private String keychainId;
    @Deprecated
    @CheckForNull
    private String keychainName;
    @CheckForNull
    private String keychainPath;
    @CheckForNull
    private Secret keychainPwd;
    @CheckForNull
    public String getProfileId() {
        return profileId;
    }

    @DataBoundSetter
    public void setProfileId(String developerProfileId) {
        this.profileId = developerProfileId;
    }

    public Boolean getImportIntoExistingKeychain() {
        return importIntoExistingKeychain == null ? Boolean.valueOf(false) : importIntoExistingKeychain;
    }

    @DataBoundSetter
    public void setImportIntoExistingKeychain(Boolean importIntoExistingKeychain ) {
        this.importIntoExistingKeychain = importIntoExistingKeychain;
    }

    @CheckForNull
    public String getKeychainId() {
        return keychainId;
    }

    @Deprecated
    @CheckForNull
    public String getKeychainName() {
        return keychainName;
    }

    @DataBoundSetter
    public void setKeychainId(String keychainId) {
        this.keychainId = keychainId;
    }

    @Deprecated
    @DataBoundSetter
    public void setKeychainName(String keychainName) {
        this.keychainName = keychainName;
    }

    @CheckForNull
    public String getKeychainPath() {
        return keychainPath;
    }

    @DataBoundSetter
    public void setKeychainPath(String keychainPath) {
        this.keychainPath = keychainPath;
    }

    @CheckForNull
    public Secret getKeychainPwd() {
        return keychainPwd;
    }

    @DataBoundSetter
    public void setKeychainPwd(Secret keychainPwd) {
        this.keychainPwd = keychainPwd;
    }

    @DataBoundConstructor
    public DeveloperProfileLoader() {
    }

    @Deprecated
    public DeveloperProfileLoader(String profileId) {
        this();
	this.profileId = profileId;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
	EnvVars envs = run.getEnvironment(listener);
	String _profileId = envs.expand(this.profileId);
        String _keychainId = envs.expand(this.keychainId);
	String _keychainName = envs.expand(this.keychainName);
	Boolean _importIntoExistingKeychain = this.importIntoExistingKeychain;
        DeveloperProfile dp = getProfile(run.getParent(), _profileId);
        if ( dp == null )
            throw new AbortException(Messages.DeveloperProfile_NoDeveloperProfileConfigured());

        String _keychainPath;
        String _keychainPwd;
        if ( BooleanUtils.isTrue(_importIntoExistingKeychain)) {
            if ( StringUtils.isNotEmpty(_keychainName) ) {
                // for backward compatibility
                listener.getLogger().println(Messages.XCodeBuilder_UseDeprecatedKeychainInfo());
                Keychain keychain = getKeychain(_keychainName);
                if (keychain == null) {
                    throw new AbortException(Messages.DeveloperProfileLoader_NoKeychainInfoConfigured());
                }
                else {
                    _keychainPath = envs.expand(keychain.getKeychainPath());
                    _keychainPwd = envs.expand(Secret.toString(keychain.getKeychainPassword()));
                    _importIntoExistingKeychain = Boolean.valueOf(true);
                }
            }
            else if ( StringUtils.isNotEmpty(_keychainId) ) {
                KeychainPasswordAndPath keychain = getKeychainPasswordAndPath(run.getParent(), _keychainId);
                if ( keychain == null ) {
                    throw new AbortException(Messages.DeveloperProfileLoader_NoKeychainInfoConfigured());
                }
                else {
                    _keychainPath = envs.expand(keychain.getKeychainPath());
                    _keychainPwd = envs.expand(keychain.getPassword().getPlainText());
                    _importIntoExistingKeychain = Boolean.valueOf(true);
                }
            }
            else {
                if ( StringUtils.isNotEmpty(this.keychainPath) && StringUtils.isNotEmpty(Secret.toString(this.keychainPwd)) ) {
                    _keychainPath = envs.expand(this.keychainPath);
                    _keychainPwd = envs.expand(Secret.toString(this.keychainPwd));
                }
                else {
                    throw new AbortException(Messages.DeveloperProfileLoader_KeychainPathOrPasswordIsBlank());
                }
            }
        }
        else {
            // Use temporary keychain with random UUID nasme.
            _keychainPath = "jenkins-" + run.getParent().getFullName().replace('/', '-');
	    _keychainPwd = UUID.randomUUID().toString();
	    _importIntoExistingKeychain = Boolean.valueOf(false);
        }

        // Note: keychain are usualy suffixed with .keychain. If we change we should probably clean up the ones we created

        ArgumentListBuilder args;

        if ( BooleanUtils.isNotTrue(_importIntoExistingKeychain) ) {
	    // if the key chain is already present, delete it and start fresh
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            args = new ArgumentListBuilder("security", "delete-keychain", _keychainPath);
            launcher.launch().cmds(args).stdout(out).join();

            args = new ArgumentListBuilder("security", "create-keychain");
            args.add("-p").addMasked(_keychainPwd);
            args.add(_keychainPath);
            invoke(launcher, listener, args, "Failed to create a keychain");
	}

        args = new ArgumentListBuilder("security", "unlock-keychain");
        args.add("-p").addMasked(_keychainPwd);
        args.add(_keychainPath);
        invoke(launcher, listener, args, "Failed to unlock keychain");

	if ( BooleanUtils.isNotTrue(_importIntoExistingKeychain) ) {
            args = new ArgumentListBuilder("security", "list-keychains");
	    args.add("-d").add("user");
	    args.add("-s").add("login.keychain");
            args.add(_keychainPath);
            invoke(launcher, listener, args, "Failed to set keychain search path");
	}

        final FilePath secret = getSecretDir(workspace, _keychainPwd);
	final byte[] dpImage = dp.getImage();
	if ( dpImage == null )
	    throw new AbortException(Messages.DeveloperProfile_NoDeveloperProfileConfigured());
        secret.unzipFrom(new ByteArrayInputStream(dpImage));

        // import identities
        for ( FilePath id : secret.list("**/*.p12") ) {
            args = new ArgumentListBuilder("security", "import");
            args.add(id).add("-k", _keychainPath);
            args.add("-P").addMasked(dp.getPassword().getPlainText());
            args.add("-T", "/usr/bin/codesign");
            args.add("-T", "/usr/bin/productsign");
            args.add(_keychainPath);
            invoke(launcher, listener, args, "Failed to import identity " + id);
        }

        {
            // display keychain info for potential troubleshooting
            args = new ArgumentListBuilder("security", "show-keychain-info");
            args.add(_keychainPath);
            ByteArrayOutputStream output = invoke(launcher, listener, args, "Failed to show keychain info");
            listener.getLogger().write(output.toByteArray());
        }

	if ( BooleanUtils.isNotTrue(_importIntoExistingKeychain) ) {
            args = new ArgumentListBuilder("security", "set-key-partition-list");
            args.add("-S").add("apple-tool:,apple:");
            args.add("-s").add("-k").addMasked(_keychainPwd);
            args.add(_keychainPath);
            invoke(launcher, listener, args, "Failed to set key partition list to keychain");
        }

	{
	    // If default keychain is not set, set the specified keychain to default keychain.
	    args = new ArgumentListBuilder("security", "default-keychain");
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
	    if ( launcher.launch().cmds(args).stdout(output).join() != 0 ) {
		listener.getLogger().write(output.toByteArray());
		String strResult = new String(output.toByteArray(), "UTF-8");
	        if ( strResult.contains("A default keychain could not be found.") ) {
		    args = new ArgumentListBuilder("security", "default-keychain");
		    args.add("-d").add("user");
		    args.add("-s").add(_keychainPath);
		    invoke(launcher, listener, args, "Failed to set default keychain");
		}
	    }
	}

	if ( BooleanUtils.isNotTrue(_importIntoExistingKeychain) ) {
	    importAppleCert(launcher, listener, workspace, _keychainPath);
	}

        // copy provisioning profiles
        VirtualChannel ch = launcher.getChannel();
        FilePath home = ch.call(new GetHomeDirectory());    // TODO: switch to FilePath.getHomeDirectory(ch) when we can
        FilePath profiles = home.child("Library/MobileDevice/Provisioning Profiles");
        profiles.mkdirs();

        for ( FilePath mp : secret.list("**/*.mobileprovision") ) {
            listener.getLogger().println(Messages.DeveloperProfile_Installing(mp.getName()));
            mp.copyTo(profiles.child(mp.getName()));
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        perform(build, build.getWorkspace(), launcher, listener);

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public GlobalConfigurationImpl getGlobalConfiguration() {
        return getDescriptor().getGlobalConfiguration();
    }

    public Keychain getKeychain(String keychainName) {
        if ( !StringUtils.isEmpty(keychainName) ) {
            for ( Keychain keychain : getGlobalConfiguration().getKeychains() ) {
                if ( keychain.getKeychainName().equals(keychainName) )
                    return keychain;
            }
        }

        if ( !StringUtils.isEmpty(this.keychainPath) ) {
            Keychain newKeychain = new Keychain();
            newKeychain.setKeychainPath(this.keychainPath);
            newKeychain.setKeychainPassword(this.keychainPwd);
            return newKeychain;
        }

        return null;
    }

    public KeychainPasswordAndPath getKeychainPasswordAndPath(Item context, String keychainId) {
        return (KeychainPasswordAndPath)CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(KeychainPasswordAndPath.class, context,
                        ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(keychainId));
    }

    public void importAppleCert(Launcher launcher, TaskListener listener, FilePath workspace, String keychainPath) throws IOException, InterruptedException {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	FilePath homeFolder = workspace.getHomeDirectory(workspace.getChannel());
	String homePath = homeFolder.getRemote();
	String cert = homePath + "/AppleWWDRCA.cer";
	launcher
	    .launch()
	    .cmds("security", "import", cert, "-k", keychainPath)
	    .stdout(out)
	    .join();
	listener.getLogger().write(out.toByteArray());
    }

    private ByteArrayOutputStream invoke(Launcher launcher, TaskListener listener, ArgumentListBuilder args, String errorMessage) throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (launcher.launch().cmds(args).stdout(output).join()!=0) {
            listener.getLogger().write(output.toByteArray());
            throw new AbortException(errorMessage);
        }
        return output;
    }

    private FilePath getSecretDir(FilePath workspace, String keychainPwd) throws IOException, InterruptedException {
        FilePath secrets = workspace.child("jenkins").child("developer-profiles");
        secrets.mkdirs();
        secrets.chmod(0700);
        return secrets.child(keychainPwd);
    }

    public DeveloperProfile getProfile(Item context, String profileId) {
        return (DeveloperProfile)CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(DeveloperProfile.class, context,
                        ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(profileId));
    }

    @Extension
    @Symbol("importDeveloperProfile")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
	GlobalConfigurationImpl globalConfiguration;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        @Inject
        void setGlobalConfiguration(GlobalConfigurationImpl c) {
            this.globalConfiguration = c;
	}

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.DeveloperProfile_ImportDeveloperProfile();
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

        public GlobalConfigurationImpl getGlobalConfiguration() {
            return globalConfiguration;
        }

        public String getUUID() {
            return "" + UUID.randomUUID().getMostSignificantBits();
        }

        public FormValidation doCheckDeveloperProfileId(@QueryParameter String value) {
            if ( StringUtils.isEmpty(value) ) {
                return FormValidation.error(Messages.DeveloperProfileLoader_MustSelectDeveloperProfile());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckKeychainPath(@QueryParameter String value, @QueryParameter String keychainName, @QueryParameter Boolean importIntoExistingKeychain) {
            if ( BooleanUtils.isTrue(importIntoExistingKeychain) ) {
                if ( StringUtils.isEmpty(keychainName) && StringUtils.isEmpty(value) ) {
                    return FormValidation.error(Messages.DeveloperProfileLoader_MustSpecifyKeychainPath());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckKeychainPwd(@QueryParameter Secret value, @QueryParameter String keychainName, @QueryParameter Boolean importIntoExistingKeychain) {
            if ( BooleanUtils.isTrue(importIntoExistingKeychain) ) {
                if ( StringUtils.isEmpty(keychainName) && StringUtils.isEmpty(Secret.toString(value)) ) {
                    return FormValidation.error(Messages.DeveloperProfileLoader_MustSpecifyKeychainPwd());
                }
            }
            return FormValidation.ok();
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
