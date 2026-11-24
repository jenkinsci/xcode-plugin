/*
 * The MIT License
 *
 * Copyright (c) 2011 Ray Yamamoto Hilton
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package au.com.rayh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import au.com.rayh.report.TestCase;
import au.com.rayh.report.TestError;
import au.com.rayh.report.TestFailure;
import au.com.rayh.report.TestSuite;

/**
 * Parse Xcode output and transform into JUnit-style xml test result files.
 * This utility class creates and manages a FilterOutputStream to parse the Xcode output to capture the
 * results of ocunit tests. 
 * @author John Bito &lt;jwbito@gmail.com&gt;
 */

public class XCodeBuildOutputParser {

    private static final DateFormat[] dateFormats = {
	new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
	new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    };
    private static final Pattern START_SUITE = Pattern.compile("Test Suite '([^\\/](?:\\.|[^'\\\\])*)'\\s+started at\\s+(.*)");
    private static final Pattern END_SUITE = Pattern.compile("Test Suite '([^\\/](?:\\.|[^'\\\\])*)'\\s+\\S+\\s+at\\s+(.*).");
    private static final Pattern START_TESTCASE = Pattern.compile("Test Case '-\\[(\\S+)\\s+(\\S+)\\]' started.");
    private static final Pattern END_TESTCASE = Pattern.compile("Test Case '-\\[(\\S+)\\s+(\\S+)\\]' passed \\((.*) seconds\\).");
    private static final Pattern ERROR_TESTCASE = Pattern.compile("(.*): error: -\\[(\\S+)\\s+(\\S+)\\] : (.*)");
    private static final Pattern ERROR_UI_TESTCASE = Pattern.compile(".*?Assertion Failure: (.+:\\d+): (.*)");
    private static final Pattern FAILED_TESTCASE = Pattern.compile("Test Case '-\\[(\\S+)\\s+(\\S+)\\]' failed \\((\\S+) seconds\\).");
    private static final Pattern FAILED_WITH_EXIT_CODE = Pattern.compile("failed with exit code (\\d+)");
    private static final Pattern TERMINATING_EXCEPTION = Pattern.compile(".*\\*\\*\\* Terminating app due to uncaught exception '(\\S+)', reason: '(.+[^\\\\])'.*");
    private File testReportsDir;
    protected OutputStream captureOutputStream;
    protected int exitCode;
    protected HashMap<String, TestSuite> testSuitesHash = new HashMap<>();
    protected TestSuite currentTestSuite = null;
    protected TestCase currentTestCase = null;
    protected boolean consoleLog;

    protected XCodeBuildOutputParser() {
        super();
    }

    /**
     * Initalize the FilterOutputStream and prepare to generate the JUnit result files
     * @param workspace directory that will receive the result files
     * @param log the Xcode output stream that should be parsed
     */
    public XCodeBuildOutputParser(File workspace, OutputStream log) {
        this();
        this.captureOutputStream = new LineBasedFilterOutputStream(log);
        this.testReportsDir = workspace;
        this.consoleLog = true;
    }

    public class LineBasedFilterOutputStream extends FilterOutputStream {
        StringBuilder buffer = new StringBuilder();

        public LineBasedFilterOutputStream(OutputStream log) {
            super(log);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            if((char)b == '\n') {
                try {
                    handleLine(buffer.toString());
                    buffer = new StringBuilder();
                } catch(Exception e) {  // Very fugly
                    throw new IOException(e);
                }
            } else {
                buffer.append((char)b);
            }
        }
    }

    private Date parseDate(String text) throws ParseException {
	Date date;
	ParseException parseException;

	date = null;
	parseException = null;

	for (DateFormat dateFormat : dateFormats) {
	    try {
		date = dateFormat.parse(text);
		break;
	    } catch (ParseException exception) {
		parseException = exception;
	    }
	}

	if ((date == null) && (parseException != null)) {
	    throw parseException;
	}

	return date;
    }

    private void requireTestSuite() {
	if(testSuitesHash.isEmpty()) {
	    throw new RuntimeException("Log statements out of sync: current test suite was empty");
	}
	if(currentTestSuite==null) {
	    throw new RuntimeException("Log statements out of sync: current test suite was null");
	}
    }

    private void requireTestSuite(String name) {
        currentTestSuite = testSuitesHash.get(name);
	if(currentTestSuite==null) {
	    // Swift 
	    String[] testSuites = name.split(Pattern.quote("."));
	    if ( testSuites.length == 2 ) {
		String xctestName = testSuites[0] + ".xctest";
		String testSuite = testSuites[1];
		if ( !testSuitesHash.containsKey(xctestName) ) {
		    throw new RuntimeException("Log statements out of sync: current test suite '" + xctestName + "' not exists");
		}
		currentTestSuite = testSuitesHash.get(testSuite);
		if ( currentTestSuite == null ) {
	    	    throw new RuntimeException("Log statements out of sync: current test suite '" + testSuite + "' not exists");
		}
	    }
	}
    }

    private void requireTestCase(String name) {
	currentTestCase = currentTestSuite.getTestCasesHash().get(name);
        if(currentTestCase==null) {
            throw new RuntimeException("Log statements out of sync: current test case '" + currentTestSuite.getName() + "." + name + "' not exists");
        }
    }

    private void writeTestReport() throws IOException, InterruptedException,
            JAXBException {
        try (OutputStream testReportOutputStream = outputForSuite()) {
            JAXBContext jaxbContext;
            Thread t = Thread.currentThread();
            ClassLoader orig = t.getContextClassLoader();
            t.setContextClassLoader(XCodeBuildOutputParser.class.getClassLoader());
            try {
                jaxbContext = JAXBContext.newInstance(TestSuite.class);
            } finally {
                t.setContextClassLoader(orig);
            }
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(currentTestSuite, testReportOutputStream);
        }
    }

    protected OutputStream outputForSuite() throws IOException,
            InterruptedException {
        return new FileOutputStream(new File(testReportsDir, "TEST-" + currentTestSuite.getName() + ".xml"));
    }

    protected void handleLine(String line) throws ParseException, IOException, InterruptedException, JAXBException {
        Matcher m = START_SUITE.matcher(line);
        if(m.matches()) {
	    if (testSuitesHash.isEmpty()) {
	    	consoleLog = true;
	    }
	    String suite_name = m.group(1);
	    if ( m.group(1).endsWith(".xctest") ) {
		suite_name = suite_name.replaceAll("-", "_");
	    }
            currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), m.group(1), parseDate(m.group(2)));
	    testSuitesHash.put(suite_name, currentTestSuite);
            return;
        }

        m = END_SUITE.matcher(line);
        if(m.matches()) {
            String suite_name = m.group(1);
            if ( m.group(1).endsWith(".xctest") ) {
                suite_name = suite_name.replaceAll("-", "_");
            }
	    requireTestSuite(suite_name);
            currentTestSuite.setEndTime(parseDate(m.group(2)));
            writeTestReport();
            testSuitesHash.remove(suite_name);
	    currentTestSuite = null;
	    if ( testSuitesHash.size() == 1 ) {
		// If the last test suite in hash is Unknown, process it and exit.
		currentTestSuite = testSuitesHash.get("UnknownSuite");
		if ( currentTestSuite != null ) {
		    currentTestSuite.setEndTime(parseDate(m.group(2)));
		    writeTestReport();
		    testSuitesHash.remove(suite_name);
		    currentTestSuite = null;
		}
	    }
	    if (testSuitesHash.isEmpty()) {
                consoleLog = false;
	    }
            return;
        }

        m = START_TESTCASE.matcher(line);
        if(m.matches()) {
	    requireTestSuite(m.group(1));
	    currentTestCase = new TestCase(m.group(1), m.group(2));
            currentTestSuite.getTestCasesHash().put(m.group(2), currentTestCase);
            return;
        }

        m = END_TESTCASE.matcher(line);
        if(m.matches()) {
            requireTestSuite(m.group(1));
            requireTestCase(m.group(2));
            currentTestCase.setTime(Float.parseFloat(m.group(3)));
            currentTestSuite.getTestCases().add(currentTestCase);
            currentTestSuite.addTest();
	    // Actually, I think that the test case should be closed and deleted here.
	    // In case the error is reported late without synchronization.
	    //currentTestSuite.getTestCasesHash().remove(m.group(2));
	    currentTestCase = null;
	    return;
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
            return;
        }

	// If the test result is returned asynchronously, there is a possibility
	//  that in this case the target test can not be decided and information
	//  is recorded in the wrong place.
        m = ERROR_UI_TESTCASE.matcher(line);
        if(m.matches()) {
            String errorLocation = m.group(1);
            String errorMessage = m.group(2);
            TestFailure failure = new TestFailure(errorMessage, errorLocation);

 	    if ( currentTestSuite == null ) {
		currentTestSuite = testSuitesHash.get("UnknownSuite");
		if ( currentTestSuite == null ) {
 		    currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), "UnknownSuite", new Date());
		    testSuitesHash.put("UnknownSuite", currentTestSuite);
		}
 	    }
                  
	    if ( currentTestCase == null ) {
		currentTestCase = currentTestSuite.getTestCasesHash().get("UnknownTestCase");
		if ( currentTestCase == null ) {
		    currentTestCase = new TestCase(currentTestSuite.getName(), "UnknownTestCase");
		    currentTestSuite.getTestCasesHash().put("UnknownTestCase", currentTestCase);
		    currentTestSuite.getTestCases().add(currentTestCase);
		    currentTestSuite.addTest();
		}
	    }

            currentTestCase.getFailures().add(failure);
            return;
        }

        m = FAILED_TESTCASE.matcher(line);
        if(m.matches()) {
            requireTestSuite(m.group(1));
            requireTestCase(m.group(2));
            currentTestSuite.addTest();
            currentTestSuite.addFailure();
            currentTestCase.setTime(Float.parseFloat(m.group(3)));
            currentTestSuite.getTestCases().add(currentTestCase);
	    //currentTestSuite.getTestCasesHash().remove(m.group(2));
	    currentTestCase = null;
            return;
        }

        m = FAILED_WITH_EXIT_CODE.matcher(line);
        if(m.matches()) {
            exitCode = Integer.parseInt(m.group(1));
            return;
        }

        if(line.matches("BUILD FAILED") || line.matches("\\*\\* TEST FAILED \\*\\*")) {
            exitCode = -1;
        }
        
        m = TERMINATING_EXCEPTION.matcher(line);
        if(m.matches()) {
            exitCode = -1;
            requireTestSuite();
            if (currentTestCase != null) {
                TestError error = new TestError(m.group(2), m.group(1));
                currentTestCase.getErrors().add(error);
                currentTestSuite.getTestCases().add(currentTestCase);
                currentTestSuite.addTest();
                currentTestSuite.addError();
		//currentTestSuite.getTestCasesHash().remove(currentTestCase.getName());
                currentTestCase = null;
            }
            writeTestReport();
            currentTestSuite = null;
        }
    }

    public OutputStream getOutputStream() {
        return captureOutputStream;
    }

    public int getExitCode() {
        return exitCode;
    }
}
