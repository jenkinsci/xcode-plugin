package au.com.rayh;

import com.google.inject.Inject;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class XcodeBuildStep extends AbstractStepImpl {
    private boolean cleanBuild = true;
    private String bundleId;
    private String buildDir = "./build";
    private String teamId;
    private String teamName;
    private String exportMethod;
    private String config = "Debug";
    private String src = "src";
    private String sdk = "iphoneos";
    private String outputDir = buildDir +  "/Release";
    private String xcodeBuildArgs = "";
    private String projectFile;
    private String schema;
    private String workspace;
    private String symRoot = buildDir + "/Products";
    private String ipaName = "app";
    private String version;
    private String shortVersion;
    private String infoPlistPath;
    private String keychainPassword;
    private String flags;
    private boolean autoSign;

    public String getConfig() {
        return config;
    }

    @DataBoundSetter
    public void setConfig(String config) {
        this.config = config;
    }

    @DataBoundSetter
    public void setCleanBuild(boolean value) {
    this.cleanBuild = value;
  }

    @DataBoundSetter
    public void setBundleId(String value) {
    this.bundleId = value;
  }

    @DataBoundSetter
    public void setBuildDir(String value) {
    this.buildDir = value;
  }

    @DataBoundSetter
    public void setTeamId(String value) {
    this.teamId = value;
  }

    @DataBoundSetter
    public void setTeamName(String value) {
    this.teamName = value;
  }

    @DataBoundSetter
    public void setExportMethod(String value) {
    this.exportMethod = value;
  }

    @DataBoundSetter
    public void setSrc(String path) {
    this.src = path;
  }

    @DataBoundSetter
    public void setSdk(String value) {
    this.sdk = value;
  }

    @DataBoundSetter
    public void setOutputDir(String value) {
    this.outputDir = value;
  }

    @DataBoundSetter
    public void setXcodeBuildArgs(String value) {
    this.xcodeBuildArgs = value;
  }

    @DataBoundSetter
    public void setProjectFile(String value) {
    this.projectFile = value;
  }

    @DataBoundSetter
    public void setSchema(String value) {
    this.schema = value;
  }

    @DataBoundSetter
    public void setWorkspace(String value) {
    this.workspace = value;
  }

    @DataBoundSetter
    public void setSymRoot(String value) {
    this.symRoot = value;
  }

    @DataBoundSetter
    public void setIpaName(String value) {
    this.ipaName = value;
  }

    @DataBoundSetter
    public void setVersion(String value) {
    this.version = value;
  }

    @DataBoundSetter
    public void setShortVersion(String value) {
    this.shortVersion = value;
  }

    @DataBoundSetter
    public void setInfoPlistPath(String value) {
    this.infoPlistPath = value;
  }

    @DataBoundSetter
    public void setKeychainPassword(String value) {
        this.keychainPassword = value;
    }

    @DataBoundSetter
    public void setAutoSign(boolean value) {
        this.autoSign = value;
    }

    @DataBoundSetter
    public void setFlags(String flags) {
        this.flags = flags;
    }

    @DataBoundConstructor
    public XcodeBuildStep() {}

    private static class XcodeBuildStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject
        private transient XcodeBuildStep step;

        @StepContextParameter
        @SuppressWarnings("unused")
        private transient Run build;

        @StepContextParameter
        @SuppressWarnings("unused")
        private transient FilePath workspace;

        @StepContextParameter
        @SuppressWarnings("unused")
        private transient Launcher launcher;

        @StepContextParameter
        @SuppressWarnings("unused")
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Void run() throws Exception {
            FilePath homeFolder = workspace.getHomeDirectory(workspace.getChannel());
            String homePath = "/" + homeFolder.getParent().getBaseName() + "/"
                    + homeFolder.getName();
            String buildName = build.getFullDisplayName();
            String _keychainName = "jenkins-" + buildName.replace('/', '-');
            Boolean buildIpa = true;
            Boolean generateArchive = false;
            Boolean cleanBeforeBuild = step.cleanBuild;
            Boolean cleanTestReports = true;
            String configuration = step.config;
            String target = null;
            String sdk = step.sdk;
            String xcodeProjectPath = step.src;
            String xcodeProjectFile = step.projectFile;
            String xcodebuildArguments = step.xcodeBuildArgs;
            String cfBundleVersionValue = step.version;
            String cfBundleShortVersionStringValue = step.shortVersion;
            Boolean unlockKeychain = true;
            String keychainName = _keychainName;
            String keychainPath = homePath + "/Library/Keychains/" + _keychainName + "-db";
            String keychainPwd = step.keychainPassword;
            String xcodeWorkspaceFile = step.workspace;
            String xcodeSchema = step.schema;
            String configurationBuildDir = step.buildDir;
            String symRoot = step.symRoot;
            Boolean allowFailingBuildResults = true;
            String ipaName = step.ipaName;
            Boolean provideApplicationVersion = true;
            String ipaOutputDirectory = step.outputDir;
            Boolean changeBundleID = true;
            String bundleID = step.bundleId;
            String bundleIDInfoPlistPath = step.infoPlistPath;
            String ipaManifestPlistUrl = "";
            Boolean interpretTargetAsRegEx = false;
            String flags = step.flags;

            XCodeBuilder builder = new XCodeBuilder(buildIpa, generateArchive, cleanBeforeBuild, cleanTestReports, configuration,
                    target, sdk, xcodeProjectPath, xcodeProjectFile, xcodebuildArguments,
                    cfBundleVersionValue, cfBundleShortVersionStringValue, unlockKeychain,
                    keychainName, keychainPath, keychainPwd, symRoot, xcodeWorkspaceFile,
                    xcodeSchema, configurationBuildDir, step.teamName, step.teamId, allowFailingBuildResults,
                    ipaName, provideApplicationVersion, ipaOutputDirectory, changeBundleID, bundleID,
                    bundleIDInfoPlistPath, ipaManifestPlistUrl, interpretTargetAsRegEx, step.exportMethod);
            builder.shouldCodeSign = step.autoSign;
            builder.setFlags(flags);

            String xcVersion = env.get("XC_VERSION");
            if (xcVersion != null) {
                this.updateXcodeVersion(xcVersion);
            }

            builder.setEnv(env);
            builder.perform(build, workspace, launcher, listener);

            return null;
        }

        public void updateXcodeVersion(String version) {
            if (this.env == null) {
                return;
            }
            String xcEnv = "/Applications/Xcode-" + version + ".app/Contents/Developer";
            env.override("DEVELOPER_DIR", xcEnv);
        }

    }

    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        private static String DISPLAY_NAME = "XCode Build";
        public DescriptorImpl() {
      super(XcodeBuildStepExecution.class);
    }

        @Override
        public String getFunctionName() {
      return "xcodeBuild";
    }

        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}