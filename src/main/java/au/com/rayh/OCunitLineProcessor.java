package au.com.rayh;

import au.com.rayh.report.TestCase;
import au.com.rayh.report.TestFailure;
import au.com.rayh.report.TestSuite;
import com.google.common.io.LineProcessor;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCunitLineProcessor implements LineProcessor<Boolean>, Serializable {

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private static Pattern START_SUITE = Pattern.compile("^Test Suite '(\\S+)'.*started at\\s+(.*)");
    private static Pattern END_SUITE = Pattern.compile("^Test Suite '(\\S+)'.*finished at\\s+(.*).");
    private static Pattern START_TESTCASE = Pattern.compile("^Test Case '-\\[\\S+\\s+(\\S+)\\]' started.");
    private static Pattern END_TESTCASE = Pattern.compile("^Test Case '-\\[\\S+\\s+(\\S+)\\]' passed \\((.*) seconds\\).");
    private static Pattern ERROR_TESTCASE = Pattern.compile("^(.*): error: -\\[(\\S+) (\\S+)\\] : (.*)");
    private static Pattern FAILED_TESTCASE = Pattern.compile("^Test Case '-\\[\\S+ (\\S+)\\]' failed \\((\\S+) seconds\\).");

    private FilePath testReportsDir;
    protected transient TestSuite currentTestSuite;
    protected transient TestCase currentTestCase;

    private static class TestReportWriter implements FilePath.FileCallable<Boolean> {

        private TestSuite testSuite;

        private TestReportWriter(TestSuite testSuite) {
            this.testSuite = testSuite;
        }

        public Boolean invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            FileOutputStream testReportOutputStream = new FileOutputStream(file);
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(TestSuite.class);
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.marshal(testSuite, testReportOutputStream);
            } catch (JAXBException e) {
                e.printStackTrace();
                return false;
            } finally {
                testReportOutputStream.close();
            }
            return true;
        }
    }

    /**
     * Initialise the FilterOutputStream and prepare to generate the JUnit result files
     * @param workspace directory that will receive the result files
     */
    public OCunitLineProcessor(FilePath workspace) {
        this.testReportsDir = new FilePath(workspace, "test-reports");
    }

    private void requireTestSuite() {
        if(currentTestSuite==null) {
            throw new RuntimeException("Log statements out of sync: current test suite was null");
        }
    }

    private void requireTestSuite(String name) {
        requireTestSuite();
        if(!currentTestSuite.getName().equals(name)) {
            throw new RuntimeException("Log statements out of sync: current test suite was '" + currentTestSuite.getName() + "' and not '" + name + "'");
        }
    }

    private void requireTestCase(String name) {
        if(currentTestCase==null) {
            throw new RuntimeException("Log statements out of sync: current test case was null");
        } else if(!currentTestCase.getName().equals(name)) {
            throw new RuntimeException("Log statements out of sync: current test case was '" + currentTestCase.getName() + "'");
        }
    }

    private void writeTestReport() throws IOException, InterruptedException, JAXBException {
        FilePath testSuitePath = new FilePath(testReportsDir, "TEST-" + currentTestSuite.getName() + ".xml");
        testSuitePath.act(new TestReportWriter(currentTestSuite));
    }

    public boolean processLine(String line) throws IOException {
        Matcher m = START_SUITE.matcher(line);
        if(m.matches()) {
            try {
                currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), m.group(1), dateFormat.parse(m.group(2)));
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        m = END_SUITE.matcher(line);
        if(m.matches()) {
            if(currentTestSuite==null) return true; // if there is no current suite, do nothing

            try {
                currentTestSuite.setEndTime(dateFormat.parse(m.group(2)));
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
            try {
                writeTestReport();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (JAXBException e) {
                e.printStackTrace();
                return false;
            }

            currentTestSuite = null;
            return true;
        }

        m = START_TESTCASE.matcher(line);
        if(m.matches()) {
            currentTestCase = new TestCase(currentTestSuite.getName(), m.group(1));
            return true;
        }

        m = END_TESTCASE.matcher(line);
        if(m.matches()) {
            requireTestSuite();
            requireTestCase(m.group(1));

            currentTestCase.setTime(Float.valueOf(m.group(2)));
            currentTestSuite.getTestCases().add(currentTestCase);
            currentTestSuite.addTest();
            currentTestCase = null;
            return true;
        }

        m = ERROR_TESTCASE.matcher(line);
        if(m.matches()) {
            String errorLocation = m.group(1);
            String testSuite = m.group(2);
            String testCase = m.group(3);
            String errorMessage = m.group(4);

            requireTestSuite(testSuite);
            requireTestCase(testCase);

            TestFailure failure = new TestFailure(errorMessage, errorLocation);
            currentTestCase.getFailures().add(failure);
            return true;
        }

        m = FAILED_TESTCASE.matcher(line);
        if(m.matches()) {
            requireTestSuite();
            requireTestCase(m.group(1));
            currentTestSuite.addTest();
            currentTestSuite.addFailure();
            currentTestCase.setTime(Float.valueOf(m.group(2)));
            currentTestSuite.getTestCases().add(currentTestCase);
            currentTestCase = null;
            return true;
        }

        return true;
    }

    public Boolean getResult() {
        return true;
    }
}
