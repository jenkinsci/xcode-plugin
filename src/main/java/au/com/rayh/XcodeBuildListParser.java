package au.com.rayh;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;


/**
 * Created by ud10404 on 5/9/14.
 */
public class XcodeBuildListParser {

    private List<String> targets = new ArrayList<>();
    private List<String> configurations = new ArrayList<>();
    private List<String> schemes = new ArrayList<>();

    public XcodeBuildListParser(String xcodebuildListOutput) {

        if(xcodebuildListOutput == null) {
            return;
        }

        String [] lines = xcodebuildListOutput.split("\n");
        List<String> curList = null;
        for(String line : lines) {
            line = line.trim();
            if (StringUtils.isEmpty(line)) {
                curList = null;
            } else if("Targets:".equals(line)) {
                curList = targets;
            } else if("Build Configurations:".equals(line)) {
                curList = configurations;
            } else if("Schemes:".equals(line)) {
                curList = schemes;
            } else if(curList != null) {
                curList.add(line);
            }
        }
    }

    public List<String> getTargets() {
        return this.targets;
    }

    public List<String> getConfigurations() {
        return this.configurations;
    }

    public List<String> getSchemes() {
        return this.schemes;
    }
}
