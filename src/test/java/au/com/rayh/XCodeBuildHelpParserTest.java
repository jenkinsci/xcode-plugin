package au.com.rayh;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Kazuhide Takahashi on 4/26/18.
 */
public class XCodeBuildHelpParserTest {

    @Test
    public void testValidOutput() throws Throwable {
        String xcodeBuildOutput =
                FileUtils.readFileToString(FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildhelp-valid.txt")));
        XcodeBuildHelpParser parser = new XcodeBuildHelpParser(xcodeBuildOutput);
        Assert.assertEquals(2, parser.getParameters().size());
        Assert.assertTrue(parser.getParameters().contains("-SampleTarget1"));
        Assert.assertTrue(parser.getParameters().contains("-TestSampleTarget1"));
    }

    @Test
    public void testInvalidOutputNull() throws Throwable {
        XcodeBuildHelpParser parser = new XcodeBuildHelpParser(null);
        Assert.assertEquals(0, parser.getParameters().size());
    }

    @Test
    public void testInvalidOutputEmpty() throws Throwable {
        String xcodeBuildOutput = FileUtils.readFileToString(
                FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildhelp-invalid.txt")));
        XcodeBuildHelpParser parser = new XcodeBuildHelpParser("");
        Assert.assertEquals(0, parser.getParameters().size());
    }

    @Test
    public void testInvalidOutputExtraLine() throws Throwable {
        String xcodeBuildOutput = FileUtils.readFileToString(
                FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildhelp-invalid.txt")));
        XcodeBuildHelpParser parser = new XcodeBuildHelpParser(xcodeBuildOutput);
        Assert.assertEquals(0, parser.getParameters().size());
    }
}
