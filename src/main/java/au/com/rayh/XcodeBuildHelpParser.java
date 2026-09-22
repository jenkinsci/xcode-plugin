package au.com.rayh;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Kazuhide Takahashi on 5/9/14.
 */
public class XcodeBuildHelpParser {

    private List<String> parameters = new ArrayList<>();

    public XcodeBuildHelpParser(String xcodebuildHelpOutput) {

        if(xcodebuildHelpOutput == null) {
            return;
        }

        String [] lines = xcodebuildHelpOutput.split("\n");
        for(String line : lines) {
            if (StringUtils.startsWith(line, "    -")) {
		String[] words = line.split(" ", 6);
		if ( words.length == 6 ) {
		   parameters.add(words[4]);
		}
            }
        }
    }

    public List<String> getParameters() {
        return this.parameters;
    }
}
