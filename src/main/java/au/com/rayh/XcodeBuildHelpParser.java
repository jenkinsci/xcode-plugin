package au.com.rayh;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kazuhide Takahashi on 5/9/14.
 */
public class XcodeBuildHelpParser {

    private List<String> parameters = new ArrayList<>();

    public XcodeBuildHelpParser(String xcodebuildHelpOutput) {

        if (xcodebuildHelpOutput == null) {
            return;
        }

        String[] lines = xcodebuildHelpOutput.split("\n");
        for (String line : lines) {
            if (line.startsWith("    -")) {
                String[] words = line.split(" ", 6);
                if (words.length == 6) {
                    parameters.add(words[4]);
                }
            }
        }
    }

    public List<String> getParameters() {
        return this.parameters;
    }
}
