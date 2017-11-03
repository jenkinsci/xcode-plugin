package au.com.rayh;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeSignOutputParser {
    private static Pattern SIGN_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-._/\\s]+: (valid on disk|satisfies its Designated Requirement)$", Pattern.MULTILINE);

    public CodeSignOutputParser() {
        super();
    }

    public boolean isValidOutput(String output) {
        Matcher m = SIGN_PATTERN.matcher(output);
        int count = 0;

        while (m.find()) {
            count++;
        }

        return count == 2;
    }
}
