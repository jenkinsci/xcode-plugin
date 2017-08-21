package au.com.rayh;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;


import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lrossett on 7/3/17.
 */
public class CodeSignWrapper extends Builder implements SimpleBuildStep {
    private static final String SIGNATURE_FILE = "_CodeSignature";
    private static final String PROFILE_FILE = "embedded.mobileprovision";
    private static final String ENTITLEMENTS_PLIST_PATH = "entitlements.plist";
    public String appPath;
    public String keychainName;
    private FilePath binaryPath;
    private FilePath resourcesPath;
    private boolean shouldClean;
    private boolean shouldVerify;
    private String ipaName;
    private  LauncherUtility runner = new LauncherUtility();
    public boolean result;

    CodeSignWrapper(String appPath, String keychainName, FilePath resourcesPath, boolean shouldClean, boolean shouldVerify, String ipaName) {
        this.appPath = appPath;
        this.keychainName = keychainName;
        this.resourcesPath = resourcesPath;
        this.shouldClean = shouldClean;
        this.shouldVerify = shouldVerify;
        this.ipaName = ipaName;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath filePath, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        this.result = _perform(build, filePath, launcher, build.getEnvironment(listener), listener);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean res = _perform(build, build.getWorkspace(), launcher, build.getEnvironment(listener), listener);
        this.result = res;
        return  res;
    }

    public boolean hasCodeSignedApp() {
        try {
            return this.binaryPath.child(SIGNATURE_FILE).exists();
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public void cleanAppSignature() throws IOException, InterruptedException {
        this.binaryPath.child(SIGNATURE_FILE).deleteRecursive();
    }

    public boolean hasCodeSignedFrameworks() throws IOException, InterruptedException {
        FilePath frameworkFolder = this.binaryPath.child("Frameworks");
        if (!frameworkFolder.exists()) {
            return false;
        }
        try {
            for (FilePath framework : frameworkFolder.list()) {
                if (framework.child(SIGNATURE_FILE).exists()) {
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }

        return false;
    }

    public void cleanFrameworksSignature() throws IOException, InterruptedException {
        FilePath frameworkFolder = this.binaryPath.child("Frameworks");
        if (!frameworkFolder.exists()) {
            return;
        }
        for (FilePath framework : frameworkFolder.list()) {
            FilePath frameworkSignature = framework.child(SIGNATURE_FILE);
            if (frameworkSignature.exists()) {
                frameworkSignature.deleteRecursive();
            }
        }
    }

    public boolean hasEmbeddedProfile() {
        try {
            return this.binaryPath.child(PROFILE_FILE).exists();
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public void cleanEmbeddedProfile() throws IOException, InterruptedException {
        this.binaryPath.child(PROFILE_FILE).delete();
    }

    public void clean() throws InterruptedException, IOException {
        if (this.hasCodeSignedApp()) {
            this.cleanAppSignature();
        }

        if (this.hasCodeSignedFrameworks()) {
            this.cleanFrameworksSignature();
        }

        if (this.hasEmbeddedProfile()) {
            this.cleanEmbeddedProfile();
        }
    }

    public boolean setEmbeddedProfile() throws IOException, InterruptedException {
        FilePath[] folders = this.resourcesPath.list("**/*.mobileprovision");

        if (folders.length == 0) {
            return false;
        }

        FilePath profilePath = folders[0];

        profilePath.copyTo(binaryPath.child(PROFILE_FILE));

        return true;
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public String getAppIdentifier(Launcher launcher, FilePath projectRoot) throws  IOException, InterruptedException {
        FilePath entitlementsPath = projectRoot.child(ENTITLEMENTS_PLIST_PATH);
        ArgumentListBuilder args = new ArgumentListBuilder("/usr/libexec/PlistBuddy", "-c", "Print :application-identifier", entitlementsPath.getRemote());
        LauncherUtility.LauncherResponse cmd = runner.run(args, false);
        return cmd.getStdout().toString().trim().replace("\\n", "");
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public void fixPlist(Launcher launcher, FilePath projectRoot) throws  IOException, InterruptedException {
        FilePath entitlementsPath = projectRoot.child(ENTITLEMENTS_PLIST_PATH);
        String appId = this.getAppIdentifier(launcher, projectRoot);
        ArgumentListBuilder args = new ArgumentListBuilder("/usr/libexec/PlistBuddy", "-c", "Add :keychain-access-groups:0 string " + appId, entitlementsPath.getRemote());
        runner.run(args, false);
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public void setEntitlements(Launcher launcher, FilePath projectRoot) throws  IOException, InterruptedException {
        String binaryPath = this.binaryPath.child(PROFILE_FILE).getRemote();
        FilePath plistPath = projectRoot.child("temp.plist");
        FilePath entitlementsPath = projectRoot.child(ENTITLEMENTS_PLIST_PATH);

        ArgumentListBuilder args = new ArgumentListBuilder("security", "cms", "-k", this.keychainName, "-D", "-i", binaryPath);
        LauncherUtility.LauncherResponse plistRes = runner.run(args, false);
        projectRoot.child("temp.plist").write(plistRes.getStdout().toString(), "utf-8");      

        ArgumentListBuilder args2 = new ArgumentListBuilder("/usr/libexec/PlistBuddy", "-x", "-c", "Print :Entitlements", plistPath.getRemote());
        LauncherUtility.LauncherResponse entitlementsRes = runner.run(args2, false);
        entitlementsPath.write(entitlementsRes.getStdout().toString(), "utf-8");
    }

    public void sign(Launcher launcher, TaskListener listener, String config, String entitlements, String target) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("codesign", "-v", "-f");

        args.add("-s").add(config);
        args.add("--keychain", this.keychainName);

        if (entitlements != null) {
            args.add("--entitlements").add(entitlements);
        }

        args.add(target);
        LauncherUtility.LauncherResponse res = runner.run(args, false);
        listener.getLogger().write(res.getStdout().toByteArray());
    }

    public void preSignFix(TaskListener listener, FilePath path) throws IOException, InterruptedException {

        List<FilePath> fileList = path.absolutize().list(new DylibFileFilter());

        if (fileList == null || fileList.isEmpty()) {
            listener.getLogger().println("No extra " + DylibFileFilter.EXTESION + " files found.");
            return;
        }

        for (FilePath filepath : fileList) {
            listener
                    .getLogger()
                    .println("Found extra " + DylibFileFilter.EXTESION + " file: " + filepath.getName());

            filepath.delete();

            listener
                    .getLogger()
                    .println(filepath.getName() + " removed before codesigning application.");
        }
    }

    public void sign(Launcher launcher, TaskListener listener, String config, String target) throws IOException, InterruptedException {
        this.sign(launcher, listener, config, null, target);
    }

    public void showValidIdentities(Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("security", "find-identity", "-p", "codesigning", "-v", this.keychainName);
        LauncherUtility.LauncherResponse res = runner.run(args, false);
        listener.getLogger().write(res.getStdout().toByteArray());
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public String getIdentity(Launcher launcher) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("security", "find-identity", "-p", "codesigning",
                "-v", this.keychainName);
        LauncherUtility.LauncherResponse res = runner.run(args, false);

        for (String part : res.getStdout().toString().split("\\n")) {
            String re = "\\s+\\d\\)\\s+([a-zA-Z0-9]+)\\s+";
            Pattern pattern = Pattern.compile(re);
            Matcher matcher = pattern.matcher(part);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public boolean checkSignature(Launcher launcher, TaskListener listener, String target) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("codesign", "--verify",
                "--deep", "--strict", "--verbose=2", target);
        LauncherUtility.LauncherResponse res = runner.run(args, false);
        listener.getLogger().write(res.getStderr().toByteArray());
        CodeSignOutputParser parser = new CodeSignOutputParser();

        if (!parser.isValidOutput(res.getStderr().toString())) {
            listener.getLogger().println("Codesign signature failed.");
            return false;
        }

        return true;
    }

    public void showAppInfo(Launcher launcher, TaskListener listener, String target) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("codesign", "-vvvv", "-d", target);
        LauncherUtility.LauncherResponse res = runner.run(args, false);
        listener.getLogger().write(res.getStderr().toByteArray());
    }

    public void createIpa(Launcher launcher, TaskListener listener, String dest) throws IOException, InterruptedException {
        FilePath payloadFolder = this.binaryPath.getParent().child("Payload");
        FilePath appFolder = payloadFolder.child(this.binaryPath.getName());

        if (payloadFolder.exists()) {
            payloadFolder.deleteRecursive();
        }

        payloadFolder.mkdirs();
        appFolder.mkdirs();
        this.binaryPath.copyRecursiveTo(appFolder);

        ArgumentListBuilder args = new ArgumentListBuilder("zip", "-r", dest + ".ipa", payloadFolder.getName());
        this.runner.setPwd(this.binaryPath.getParent());
        LauncherUtility.LauncherResponse res = runner.run(args, false);
        listener.getLogger().write(res.getStdout().toByteArray());
        this.runner.setPwd(null);
    }

    public String getIpaName() {
        String remotePath = this.binaryPath.getRemote();

        if (this.ipaName != null) {
            return remotePath.replace(this.binaryPath.getName(), ipaName.replace(".ipa", ""));
        }

        return remotePath.replace(".app", "");
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private boolean _perform(Run<?,?> build, FilePath projectRoot, Launcher launcher, EnvVars envs, TaskListener listener) throws InterruptedException, IOException {
        String path = this.appPath;

        if (path == null) {
            return false;
        }

        FilePath binaryPath = projectRoot.child(path);

        if (!binaryPath.exists()) {
            listener.getLogger().println("Could not find binary at path: " + binaryPath);
            return false;
        }

        this.binaryPath = binaryPath;

        if (this.shouldClean) {
            this.clean();
        }

        this.runner.bootstrap(launcher, listener);
        this.runner.setEnv(envs);

        if (!this.setEmbeddedProfile()) {
            listener.getLogger().println("Provisioning profile not found");
            return false;
        }
        this.setEntitlements(launcher, projectRoot);
        this.fixPlist(launcher, projectRoot);
        this.showValidIdentities(launcher, listener);

        String identifier = this.getIdentity(launcher);

        if (identifier == null) {
            listener.getLogger().println("Could not retrieve a valid codesign identity.");
            return false;
        }

        this.preSignFix(listener, binaryPath);

        FilePath frameworkFolder = this.binaryPath.child("Frameworks");
        if (frameworkFolder.exists()) {
            for (FilePath framework : frameworkFolder.list()) {
                this.sign(launcher, listener, identifier, framework.getRemote());
            }
        }

        String entitlementsPath = projectRoot.child(ENTITLEMENTS_PLIST_PATH).getRemote();
        this.sign(launcher, listener, identifier, entitlementsPath , binaryPath.getRemote());

        if (this.shouldVerify) {
            if (!this.checkSignature(launcher, listener, binaryPath.getRemote())) {
                return false;
            }
        }

        this.showAppInfo(launcher, listener, this.binaryPath.getRemote());

        this.createIpa(launcher, listener, this.getIpaName());

        return true;
    }

    public GlobalConfigurationImpl getGlobalConfiguration() {
        return getDescriptor().getGlobalConfiguration();
    }

    @Override
    public CodeSignWrapper.DescriptorImpl getDescriptor() {
        return (CodeSignWrapper.DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        GlobalConfigurationImpl globalConfiguration;

        @Override
        public String getDisplayName() {
            return "CodeSign";
        }

        protected DescriptorImpl(Class<? extends Builder> clazz) {
            super(clazz);
        }

        public DescriptorImpl() {
            load();
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        @Inject
        void setGlobalConfiguration(GlobalConfigurationImpl c) {
            this.globalConfiguration = c;
        }

        public GlobalConfigurationImpl getGlobalConfiguration() {
            return globalConfiguration;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
