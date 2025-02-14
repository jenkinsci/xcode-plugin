package au.com.rayh;

import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import hudson.FilePath;
import hudson.Launcher.LocalLauncher;
import hudson.util.StreamTaskListener;

/**
 * Created by Kazuhide Takahashi on 1/7/19.
 */
public class XcodeTestSummariesParserTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    @Test
    public void testValidTestSummaries() throws Throwable {
	File dir = tmp.getRoot();
	FilePath workspace = new FilePath(dir);
	String projectLocation = URLDecoder.decode(XcodeTestSummariesParserTest.class.getClassLoader().getResource("XcodeTestSummaries.tar.gz").getPath(), "UTF-8");
	run(workspace, "tar", "zxvpf", projectLocation);
	FilePath testSummariesPath = workspace.child("result/TestSummaries.plist");
	Assert.assertNotNull(testSummariesPath);
        XcodeTestSummariesParser parser = new XcodeTestSummariesParser(new FilePath(new File(".")));
	Assert.assertNotNull(parser);
	parser.parseTestSummariesPlist(testSummariesPath);
    }

    @Test
    public void testInvalidTestSummaries() throws Throwable {

    }

    private static void run(FilePath dir, String... cmds) throws InterruptedException {
        try {
            Assert.assertEquals(0, new LocalLauncher(StreamTaskListener.fromStdout()).launch().cmds(cmds).pwd(dir).join());
        } catch (IOException x) { // perhaps restrict to x.message.contains("Cannot run program")? or "error=2, No such file or directory"?
            Assume.assumeNoException("failed to run " + Arrays.toString(cmds), x);
        }
    }
}
