package au.com.rayh;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by timothy on 9/6/14.
 */
public class XCodeBuildListParserTest {

    @Test
    public void testValidOutput() throws Throwable {
        String xcodeBuildOutput =
                FileUtils.readFileToString(FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildlist-valid.txt")));

        XcodeBuildListParser parser = new XcodeBuildListParser(xcodeBuildOutput);

        Assert.assertEquals(4, parser.getTargets().size());
        Assert.assertEquals(2, parser.getConfigurations().size());
        Assert.assertEquals(2, parser.getSchemes().size());

        Assert.assertTrue(parser.getTargets().contains("SampleTarget1"));
        Assert.assertTrue(parser.getTargets().contains("SampleTarget2"));
        Assert.assertTrue(parser.getTargets().contains("TestSampleTarget1"));
        Assert.assertTrue(parser.getTargets().contains("TestSampleTarget2"));
        Assert.assertTrue(parser.getConfigurations().contains("BuildConfiguration1"));
        Assert.assertTrue(parser.getConfigurations().contains("BuildConfiguration2"));
        Assert.assertTrue(parser.getSchemes().contains("SampleScheme1"));
        Assert.assertTrue(parser.getSchemes().contains("SampleScheme2"));
    }

    @Test
    public void testInvalidOutputNull() throws Throwable {
        XcodeBuildListParser parser = new XcodeBuildListParser(null);

        Assert.assertEquals(0, parser.getTargets().size());
        Assert.assertEquals(0, parser.getConfigurations().size());
        Assert.assertEquals(0, parser.getSchemes().size());
    }

    @Test
    public void testInvalidOutputEmpty() throws Throwable {
        String xcodeBuildOutput = FileUtils.readFileToString(
                FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildlist-invalid1.txt")));

        XcodeBuildListParser parser = new XcodeBuildListParser("");

        Assert.assertEquals(0, parser.getTargets().size());
        Assert.assertEquals(0, parser.getConfigurations().size());
        Assert.assertEquals(0, parser.getSchemes().size());
    }

    @Test
    public void testInvalidOutputExtraLine() throws Throwable {
        String xcodeBuildOutput = FileUtils.readFileToString(
                FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildlist-invalid1.txt")));

        XcodeBuildListParser parser = new XcodeBuildListParser(xcodeBuildOutput);

        Assert.assertEquals(0, parser.getTargets().size());
        Assert.assertEquals(0, parser.getConfigurations().size());
        Assert.assertEquals(0, parser.getSchemes().size());
    }

    @Test
    public void testInvalidOutputMissngColon() throws Throwable {
        String xcodeBuildOutput = FileUtils.readFileToString(
                FileUtils.toFile(ClassLoader.getSystemResource("xcodebuildlist-invalid2.txt")));

        XcodeBuildListParser parser = new XcodeBuildListParser(xcodeBuildOutput);

        Assert.assertEquals(0, parser.getTargets().size());
        Assert.assertEquals(0, parser.getConfigurations().size());
        Assert.assertEquals(0, parser.getSchemes().size());
    }
}
