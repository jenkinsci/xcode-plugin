package au.com.rayh;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import hudson.FilePath;

import com.dd.plist.NSDictionary; 
import com.dd.plist.NSArray;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import au.com.rayh.report.TestCase;
import au.com.rayh.report.TestFailure;
import au.com.rayh.report.TestSuite;

/**
 * Summary of test results output by xcodebuild Analyze "TestSummaries.plist" to generate a JUnit compatible XML file.
 * @author Kazuhide Takahashi
 */
public class XcodeTestSummariesParser {
    private FilePath testReportsDir;
    //private static Pattern ASSERTION_FAILURE = Pattern.compile("(Assertion Failure: .*?)\\{\\(\\n(.*?)\\n\\)\\}", Pattern.DOTALL);
    private static Pattern FAILED_MESSAGE = Pattern.compile("(failed: .*?)\\n\\((.*?)\\n\\)", Pattern.DOTALL);

    public XcodeTestSummariesParser(FilePath workspace) throws IOException, InterruptedException {
	super();
	testReportsDir = workspace.child("test-reports");
	testReportsDir.mkdirs();
    }

    private void writeTestReport(TestSuite currentTestSuite) throws IOException, InterruptedException,
            JAXBException {
        try (OutputStream testReportOutputStream = outputForSuite(currentTestSuite)) {
            JAXBContext jaxbContext;
            Thread t = Thread.currentThread();
            ClassLoader orig = t.getContextClassLoader();
            t.setContextClassLoader(XcodeTestSummariesParser.class.getClassLoader());
            try {
                jaxbContext = JAXBContext.newInstance(TestSuite.class);
            } finally {
                t.setContextClassLoader(orig);
            }
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(currentTestSuite, testReportOutputStream);
        }
    }

    protected OutputStream outputForSuite(TestSuite currentTestSuite) throws IOException, InterruptedException {
	return testReportsDir.child("TEST-" + currentTestSuite.getName() + ".xml").write();
    }

    private static void addFailureSummaries(NSObject[] failureSummaries, TestCase currentTestCase) {
	for ( NSObject object:failureSummaries ) {
	    NSDictionary failureSummarie = (NSDictionary)object;
	    boolean performanceFailure = ((NSNumber)failureSummarie.objectForKey("PerformanceFailure")).boolValue();
	    if ( !performanceFailure ) {
		TestFailure failure;
		String message = failureSummarie.objectForKey("Message").toString();
		Matcher m = FAILED_MESSAGE.matcher(message);
		if ( m.matches() ) {
		    String errorMessage = m.group(1);
		    String stackTrace = m.group(2);
		    failure = new TestFailure(errorMessage, stackTrace);
		}
		else {
		    String fileName = failureSummarie.objectForKey("FileName").toString();
		    String lineNumber = failureSummarie.objectForKey("LineNumber").toString();
		    failure = new TestFailure(message + "\n at File: " + fileName + "\n Line number: " + lineNumber, "No stacktrace here.");
		}
		currentTestCase.getFailures().add(failure);
	    }
	}
    }

    /*
    private static void addActivitySummaries(NSObject[] activitySummaries, TestCase currentTestCase) {
        for ( NSObject object:activitySummaries ) {
	    NSDictionary summarie = (NSDictionary)object;
	    NSObject value = summarie.objectForKey("Title");
            if ( value != null ) {
		String title = value.toString();
		Matcher m = ASSERTION_FAILURE.matcher(title);
		if ( m.matches() ) {
		    String errorLocation = m.group(1);
		    String errorMessage = m.group(2);
		    TestFailure failure = new TestFailure(errorMessage, errorLocation);
		    currentTestCase.getFailures().add(failure);
		}
            }
        }
    }
    */

    /**
     * @param tests An array of NSDictionaries containing the results of subtests.
     * @param parentTestSuite An instance of the parent test result including this subtests.
     */
    public void parseSubTests(NSObject[] tests, TestSuite parentTestSuite) throws ParseException, IOException, InterruptedException, JAXBException {
	for ( NSObject object:tests ) {
            NSDictionary test = (NSDictionary)object;
	    Float duration = ((NSNumber)test.objectForKey("Duration")).floatValue();
	    String testName = test.objectForKey("TestName").toString();
	    String testIdentifier = test.objectForKey("TestIdentifier").toString();
	    String testObjectClass = test.objectForKey("TestObjectClass").toString();
	    NSObject value = test.objectForKey("Subtests");
            if ( value == null ) {
		TestCase currentTestCase = new TestCase(parentTestSuite.getName(), testName);
		String testStatus = test.objectForKey("TestStatus").toString();
		if ( testStatus.equals("Failure") ) {
		    value = test.objectForKey("FailureSummaries");
		    if ( value != null ) {
			NSObject[] failureSummaries = ((NSArray)value).getArray();
			addFailureSummaries(failureSummaries, currentTestCase);
		    }
		    /*
		    value = test.objectForKey("ActivitySummaries");
		    if ( value != null ) {
			NSObject[] activitySummaries = ((NSArray)value).getArray();
			addActivitySummaries(activitySummaries, currentTestCase);
		    }
		    */
		    currentTestCase.setTime(duration);
		    parentTestSuite.getTestCases().add(currentTestCase);
		    parentTestSuite.addFailure();
		}
		else if ( testStatus.equals("Success") ) {
		    currentTestCase.setTime(duration);
		    parentTestSuite.getTestCases().add(currentTestCase);
		    parentTestSuite.addTest();
		}
		writeTestReport(parentTestSuite);
	    }
	    else {
		TestSuite currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), testName, null);
		currentTestSuite.setDuration(duration);
		NSObject[] subTests = ((NSArray)value).getArray();
		parseSubTests(subTests, currentTestSuite);
            }
	}
    }

    /**
     * @param testSummariesPlistFile The location of the TestSummaries.plist file output from Xcode.
     */
    public void parseTestSummariesPlist(FilePath testSummariesPlistFile) {
	try {
	    NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(testSummariesPlistFile.read());
	    NSObject[] testableSummaries = ((NSArray)rootDict.objectForKey("TestableSummaries")).getArray();
	    for ( NSObject object:testableSummaries ) {
		NSDictionary testSummarie = (NSDictionary)object;
		String diagnosticsDirectory = testSummarie.objectForKey("DiagnosticsDirectory").toString();
		String testName = testSummarie.objectForKey("TestName").toString();
		String projectPath = testSummarie.objectForKey("ProjectPath").toString();
		String targetName = testSummarie.objectForKey("TargetName").toString();
		TestSuite currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), testName, null);
		NSObject[] tests = ((NSArray)testSummarie.objectForKey("Tests")).getArray();
		if ( tests != null ) {
		    parseSubTests(tests, currentTestSuite);
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
    }
}
