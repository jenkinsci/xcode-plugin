package hudson.plugins.xcode;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Information about Xcode installation.
 *
 * @author Kazuhide Takahashi
 */
public class XcodeInstallation extends ToolInstallation implements NodeSpecific<XcodeInstallation>, EnvironmentSpecific<XcodeInstallation> {

    /**
     * Constructor for XcodeInstallation.
     *
     * @param name Tool name (for example, "Xcode 9.3")
     * @param home Tool location (usually "/Applications/Xcode.app/Contents/Developer")
     * @param properties {@link java.util.List} of properties for this tool
     */
    @DataBoundConstructor
    public XcodeInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public static XcodeInstallation[] allInstallations() {
	Jenkins instance = Jenkins.getInstance();
	if ( instance != null ) {
	    DescriptorImpl descriptor = instance.getDescriptorByType(DescriptorImpl.class);
	    if ( descriptor != null ) {
    		XcodeInstallation[] installations = descriptor.getInstallations();
    		if ( installations != null ) {
		    return installations;
		}
	    }
    	}
 	return new XcodeInstallation[0];
   }

    /**
     * getXcodebuild.
     *
     * @return {@link java.lang.String} that will be used to execute xcodebuild (e.g. "/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild")
     */
    public String getXcodebuild() {
        return getHome() + "/usr/bin/xcodebuild";
    }

    private static XcodeInstallation[] getInstallations(DescriptorImpl descriptor) {
        XcodeInstallation[] installations = null;
        try {
            installations = descriptor.getInstallations();
        } catch (NullPointerException e) {
            installations = new XcodeInstallation[0];
        }
        return installations;
    }

    public XcodeInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new XcodeInstallation(getName(), translateFor(node, log), Collections.<ToolProperty<?>>emptyList());
    }

    public XcodeInstallation forEnvironment(EnvVars environment) {
        return new XcodeInstallation(getName(), environment.expand(getHome()), Collections.<ToolProperty<?>>emptyList());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance == null) {
            /* Throw AssertionError exception to match behavior of Jenkins.getDescriptorOrDie */
            throw new AssertionError("No Jenkins instance");
        }
        return (DescriptorImpl) jenkinsInstance.getDescriptorOrDie(getClass());
    }

    @Extension @Symbol("xcode")
    public static class DescriptorImpl extends ToolDescriptor<XcodeInstallation> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Xcode tools";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            setInstallations(req.bindJSONToList(clazz, json.get("tool")).toArray(new XcodeInstallation[0]));
            save();
            return true;
        }

        public FormValidation doCheckHome(@QueryParameter File value) {
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            String path = value.getPath() + "/usr/bin/xcodebuild";

            return FormValidation.validateExecutable(path);
        }

        public XcodeInstallation getInstallation(String name) {
            for(XcodeInstallation i : getInstallations()) {
                if(i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }
    }
}
