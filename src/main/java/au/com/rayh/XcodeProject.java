package au.com.rayh;

//import org.kohsuke.stapler.DataBoundConstructor;
import java.util.HashMap;

public class XcodeProject {
 
    public HashMap<String, ProjectTarget> projectTarget;

    XcodeProject() {
        projectTarget = new HashMap<>();
    }
}

