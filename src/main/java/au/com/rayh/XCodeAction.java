package au.com.rayh;

import hudson.model.Action;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Simple Action for storing the build number
 * @author Reuben Bijl
 */
public class XCodeAction implements Action {
    private String buildDescription;

    @DataBoundConstructor
    public XCodeAction(String buildDescription) {
        this.buildDescription = buildDescription;
    }

    public String getUrlName() { return ""; }
    public String getDisplayName() { return ""; }
    public String getIconFileName() { return ""; }
    public  String getBuildDescription() { return buildDescription; }
}