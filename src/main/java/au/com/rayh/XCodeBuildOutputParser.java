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

	private static DateFormat[] dateFormats = {
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
	};
    private static Pattern START_SUITE = Pattern.compile("Test Suite '([^/].+)'.*started at\\s+(.*)");
    private static Pattern END_SUITE = Pattern.compile("Test Suite '([^/].+)'.*\\S+ at\\s+(.*).");
    private static Pattern START_TESTCASE = Pattern.compile("Test Case '-\\[\\S+\\s+(\\S+)\\]' started.");
    private static Pattern END_TESTCASE = Pattern.compile("Test Case '-\\[\\S+\\s+(\\S+)\\]' passed \\((.*) seconds\\).");
    private static Pattern ERROR_TESTCASE = Pattern.compile("(.*): error: -\\[(\\S+) (\\S+)\\] : (.*)");
    private static Pattern ERROR_UI_TESTCASE = Pattern.compile(".*?Assertion Failure: (.+:\\d+): (.*)");
    private static Pattern FAILED_TESTCASE = Pattern.compile("Test Case '-\\[\\S+ (\\S+)\\]' failed \\((\\S+) seconds\\).");
    private static Pattern FAILED_WITH_EXIT_CODE = Pattern.compile("failed with exit code (\\d+)");
    private static Pattern TERMINATING_EXCEPTION = Pattern.compile(".*\\*\\*\\* Terminating app due to uncaught exception '(\\S+)', reason: '(.+[^\\\\])'.*");
    private File testReportsDir;
    protected OutputStream captureOutputStream;
    protected int exitCode;
    protected TestSuite currentTestSuite;
    protected TestCase currentTestCase;
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
        if(currentTestSuite==null) {
            throw new RuntimeException("Log statements out of sync: current test suite was null");
        }
    }

    private void requireTestSuite(String name) {
        requireTestSuite();
        if(name == null || !name.endsWith(currentTestSuite.getName())) {
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

    private void writeTestReport() throws IOException, InterruptedException,
            JAXBException {
        try (OutputStream testReportOutputStream = outputForSuite()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(TestSuite.class);
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
        	  consoleLog = true;
            currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), m.group(1), parseDate(m.group(2)));
            return;
        }

        m = END_SUITE.matcher(line);
        if(m.matches()) {
            if(currentTestSuite==null) return; // if there is no current suite, do nothing

            currentTestSuite.setEndTime(parseDate(m.group(2)));
            writeTestReport();

            currentTestSuite = null;
            consoleLog = false;
            return;
        }

        m = START_TESTCASE.matcher(line);
        if(m.matches()) {
            currentTestCase = new TestCase(currentTestSuite.getName(), m.group(1));
            return;
        }

        m = END_TESTCASE.matcher(line);
        if(m.matches()) {
            requireTestSuite();
            requireTestCase(m.group(1));

            currentTestCase.setTime(Float.valueOf(m.group(2)));
            currentTestSuite.getTestCases().add(currentTestCase);
            currentTestSuite.addTest();
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
	
        m = ERROR_UI_TESTCASE.matcher(line);
        if(m.matches()) {
            String errorLocation = m.group(1);
            String errorMessage = m.group(2);

            TestFailure failure = new TestFailure(errorMessage, errorLocation);
            currentTestCase.getFailures().add(failure);
            return;
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
