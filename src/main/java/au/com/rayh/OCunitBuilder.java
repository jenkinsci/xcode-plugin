package au.com.rayh;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Emanuele Zattin
 */
@SuppressWarnings("UnusedDeclaration")
public class OCunitBuilder extends Builder {

    @DataBoundConstructor
    public OCunitBuilder() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Starting to parse for OCunit... ");
        Files.readLines(build.getLogFile(), Charsets.UTF_8, new OCunitLineProcessor(build.getWorkspace()));
        listener.getLogger().println("Done.");
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.XCodeBuilder_ocunit();
        }
    }
}
