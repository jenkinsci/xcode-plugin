package au.com.rayh;

import com.google.inject.Inject;
import hudson.*;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

/**
 * Created by lrossett on 7/4/17.
 */
public class CodeSignStep extends AbstractStepImpl {
    private String profileId;
    private boolean clean = true;
    private boolean verify = true;
    private String ipaName;
    private String appPath;

    @DataBoundSetter
    public void setProfileId(String value) {
        this.profileId = value;
    }

    @DataBoundSetter
    public void setClean(boolean value) {
        this.clean = value;
    }

    @DataBoundSetter
    public  void setVerify(boolean value) {
        this.verify = value;
    }

    @DataBoundSetter
    public void setIpaName(String value) {
        this.ipaName = value;
    }

    @DataBoundSetter
    public void setAppPath(String value) {
        this.appPath = value;
    }

    public String getProfileId() {
        return profileId;
    }

    public boolean isClean() {
        return clean;
    }

    public boolean isVerify() {
        return verify;
    }

    public String getIpaName() {
        return ipaName;
    }

    public String getAppPath() {
        return appPath;
    }

    @DataBoundConstructor
    public CodeSignStep() {}

    private static class CodeSignStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject
        private transient CodeSignStep step;

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
            String profileId = step.profileId;
            boolean clean = step.clean;
            boolean verify = step.verify;
            String ipaName = step.ipaName;
            String appPath = step.appPath;
            String buildName = build.getFullDisplayName();
            String keychainName = "jenkins-" + buildName.replace('/', '-');

            DeveloperProfileLoader profileLoader = new DeveloperProfileLoader(profileId);
            profileLoader.setProjectScope(false);
            profileLoader.perform(build, workspace, launcher, listener);

            CodeSignWrapper codesign = new CodeSignWrapper(appPath,
                    keychainName,
                    profileLoader.getSecretDir(workspace),
                    clean,
                    verify,
                    ipaName);
            codesign.perform(build, workspace, launcher, listener);

            profileLoader.unload(workspace, launcher, listener);

            if (!codesign.result) {
                throw  new AbortException("Error while running the build job.");
            }

            return null;
        }
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        private static String DISPLAY_NAME = "CodeSign";
        private static String FN_NAME = "codeSign";

        public DescriptorImpl() {
            super(CodeSignStep.CodeSignStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return FN_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
