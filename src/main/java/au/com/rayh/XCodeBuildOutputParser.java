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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.rayh;

import au.com.rayh.report.TestCase;
import au.com.rayh.report.TestFailure;
import au.com.rayh.report.TestSuite;
import hudson.FilePath;
import hudson.model.TaskListener;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 *
 * @author ray
 */
public class XCodeBuildOutputParser {
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private static Pattern START_SUITE = Pattern.compile("Test Suite '(\\S+)'.*started at\\s+(.*)");
    private static Pattern END_SUITE = Pattern.compile("Test Suite '(\\S+)'.*finished at\\s+(.*).");
    private static Pattern START_TESTCASE = Pattern.compile("Test Case '-\\[\\S+\\s+(\\S+)\\]' started.");
    private static Pattern END_TESTCASE = Pattern.compile("Test Case '-\\[\\S+\\s+(\\S+)\\]' passed \\((.*) seconds\\).");
    private static Pattern ERROR_TESTCASE = Pattern.compile("(.*): error: -\\[(\\S+) (\\S+)\\] : (.*)");
    private static Pattern FAILED_TESTCASE = Pattern.compile("Test Case '-\\[\\S+ (\\S+)\\]' failed \\((\\S+) seconds\\).");
    private static Pattern FAILED_WITH_EXIT_CODE = Pattern.compile("failed with exit code (\\d+)");

    FilePath testReportsDir;
    OutputStream captureOutputStream;
    TaskListener buildListener;

    int exitCode;
    TestSuite currentTestSuite;
    TestCase currentTestCase;

    public XCodeBuildOutputParser(FilePath workspace, TaskListener buildListener) throws IOException, InterruptedException {
        this.buildListener = buildListener;
        this.captureOutputStream = new LineBasedFilterOutputStream();

        testReportsDir = workspace.child("test-reports");
        testReportsDir.mkdirs();
    }

    public class LineBasedFilterOutputStream extends FilterOutputStream {
        StringBuilder buffer = new StringBuilder();

        public LineBasedFilterOutputStream() {
            super(buildListener.getLogger());
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            if((char)b == '\n') {
                try {
                    handleLine(buffer.toString());
                    buffer = new StringBuilder();
                } catch(Exception e) {  // Very fugly
                    buildListener.fatalError(e.getMessage(), e);
                    throw new IOException(e);
                }
            } else {
                buffer.append((char)b);
            }
        }
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
        OutputStream testReportOutputStream = testReportsDir.child("TEST-" + currentTestSuite.getName() + ".xml").write();
        JAXBContext jaxbContext = JAXBContext.newInstance(TestSuite.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(currentTestSuite, testReportOutputStream);

    }

    protected void handleLine(String line) throws ParseException, IOException, InterruptedException, JAXBException {
        Matcher m = START_SUITE.matcher(line);
        if(m.find()) {
            currentTestSuite = new TestSuite(InetAddress.getLocalHost().getHostName(), m.group(1), dateFormat.parse(m.group(2)));
            return;
        }

        m = END_SUITE.matcher(line);
        if(m.find()) {
            if(currentTestSuite==null) return; // if there is no current suite, do nothing

            currentTestSuite.setEndTime(dateFormat.parse(m.group(2)));
            writeTestReport();

            currentTestSuite = null;
            return;
        }

        m = START_TESTCASE.matcher(line);
        if(m.find()) {
            currentTestCase = new TestCase(currentTestSuite.getName(), m.group(1));
            return;
        }

        m = END_TESTCASE.matcher(line);
        if(m.find()) {
            requireTestSuite();
            requireTestCase(m.group(1));

            currentTestCase.setTime(Float.valueOf(m.group(2)));
            currentTestSuite.getTestCases().add(currentTestCase);
            currentTestSuite.addTest();
            currentTestCase = null;
            return;
        }

        m = ERROR_TESTCASE.matcher(line);
        if(m.find()) {

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

        m = FAILED_TESTCASE.matcher(line);
        if(m.find()) {
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
        if(m.find()) {
            exitCode = Integer.valueOf(m.group(1));
            return;
        }

        if(line.contains("BUILD FAILED")) {
            exitCode = -1;
        }
    }

    public OutputStream getOutputStream() {
        return captureOutputStream;
    }

    public int getExitCode() {
        return exitCode;
    }
}
