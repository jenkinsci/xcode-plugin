package au.com.rayh;

import hudson.model.InvisibleAction;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Simple Action for storing the build number
 * @author Reuben Bijl
 */
public class XCodeAction extends InvisibleAction {
    private String buildDescription;

    @DataBoundConstructor
    public XCodeAction(String buildDescription) {
        this.buildDescription = buildDescription;
    }

    public String getBuildDescription() {
        return buildDescription;
    }
}
