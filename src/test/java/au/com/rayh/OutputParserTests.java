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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;

import au.com.rayh.report.TestCase;
import au.com.rayh.report.TestSuite;

/**
 *
 * @author ray
 */
class OutputParserTests {
    XCodeBuildOutputParser parser;

    OutputParserTests(XCodeBuildOutputParser theParser) {
    	parser = theParser;
    }

	void shouldIgnoreStartSuiteLineThatContainsFullPath() throws Exception {
        String line = "Test Suite '/Users/ray/Development/Projects/Java/xcodebuild-hudson-plugin/work/jobs/PBS Streamer/workspace/build/Debug-iphonesimulator/TestSuite.octest(Tests)' started at 2010-10-02 13:39:22 GMT 0000";
        parser.handleLine(line);
        assertNull(parser.currentTestSuite);
    }

	void shouldParseStartTestSuite() throws Exception {
        String line = "Test Suite 'PisClientTestCase' started at 2010-10-02 13:39:23 GMT 0000";
        parser.handleLine(line);
        assertNotNull(parser.currentTestSuite);
        assertEquals("PisClientTestCase", parser.currentTestSuite.getName());
        assertEquals(new Date(Date.UTC(110, 9, 2, 13, 39, 23)), parser.currentTestSuite.getStartTime());
    }

	void shouldParseEndTestSuite() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "PisClientTestCase", new Date());
        String line = "Test Suite 'PisClientTestCase' finished at 2010-10-02 13:41:23 GMT 0000.";
        parser.handleLine(line);
        assertNull(parser.currentTestSuite);
        assertEquals(0, parser.exitCode);
    }

	void shouldParseStartTestSuiteXC() throws Exception {
        String line = "Test Suite 'All tests' started at 2014-12-12 05:12:52 +0000";
        parser.handleLine(line);
        assertNotNull(parser.currentTestSuite);
        assertEquals("All tests", parser.currentTestSuite.getName());
        assertEquals(new Date(Date.UTC(114, 11, 12, 05, 12, 52)), parser.currentTestSuite.getStartTime());
    }

	void shouldParseEndTestSuiteXC() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "All tests", new Date());
        String line = "Test Suite 'All tests' passed at 2014-12-12 05:12:52 +0000.";
        parser.handleLine(line);
        assertNull(parser.currentTestSuite);
        assertEquals(0, parser.exitCode);
    }

	void shouldParseStartTestCase() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "PisClientTestCase", new Date());
        String line = "Test Case '-[PisClientTestCase testThatFails]' started.";
        parser.handleLine(line);
        assertNotNull(parser.currentTestCase);
        assertEquals("testThatFails", parser.currentTestCase.getName());
    }

	void shouldAddErrorToTestCase() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "PisClientTestCase", new Date());
        parser.currentTestCase = new TestCase("PisClientTestCase", "testThatFails");
        String line = "/Users/ray/Development/Projects/Java/xcodebuild-hudson-plugin/work/jobs/PBS Streamer/workspace/PisClientTestCase.m:21: error: -[PisClientTestCase testThatFails] : \"((nil) != nil)\" should be true. This always fails";
        parser.handleLine(line);
        assertEquals(1, parser.currentTestCase.getFailures().size());
        assertEquals("/Users/ray/Development/Projects/Java/xcodebuild-hudson-plugin/work/jobs/PBS Streamer/workspace/PisClientTestCase.m:21", parser.currentTestCase.getFailures().get(0).getLocation());
        assertEquals("\"((nil) != nil)\" should be true. This always fails", parser.currentTestCase.getFailures().get(0).getMessage());
    }
	
	void shouldAddUIErrorToTestCase() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "PisClientTestCase", new Date());
        parser.currentTestCase = new TestCase("PisClientTestCase", "testThatFails");
        String line = "t =    29.77s             Assertion Failure: AppUITests.m:31: UI Testing Failure - No matches found for Alert";
        parser.handleLine(line);
        assertEquals(1, parser.currentTestCase.getFailures().size());
        assertEquals("AppUITests.m:31", parser.currentTestCase.getFailures().get(0).getLocation());
        assertEquals("UI Testing Failure - No matches found for Alert", parser.currentTestCase.getFailures().get(0).getMessage());
    }

	void shouldParsePassedTestCase() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "PisClientTestCase", new Date());
        parser.currentTestCase = new TestCase("PisClientTestCase","testThatPasses");
        String line = "Test Case '-[PisClientTestCase testThatPasses]' passed (1.234 seconds).";
        parser.handleLine(line);
        assertNull(parser.currentTestCase);
        assertEquals(1, parser.currentTestSuite.getTestCases().size());
        assertEquals("testThatPasses", parser.currentTestSuite.getTestCases().get(0).getName());
        assertEquals(1.234f, parser.currentTestSuite.getTestCases().get(0).getTime(),0);
        assertEquals(1,parser.currentTestSuite.getTests());
        assertEquals(0,parser.currentTestSuite.getFailures());
    }

	void shouldParseFailedTestCase() throws Exception {
        parser.currentTestSuite = new TestSuite("host", "PisClientTestCase", new Date());
        parser.currentTestCase = new TestCase("PisClientTestCase","testThatFails");
        String line = "Test Case '-[PisClientTestCase testThatFails]' failed (1.234 seconds).";
        parser.handleLine(line);
        assertNull(parser.currentTestCase);
        assertEquals(1, parser.currentTestSuite.getTestCases().size());
        assertEquals("testThatFails", parser.currentTestSuite.getTestCases().get(0).getName());
        assertEquals(1.234f, parser.currentTestSuite.getTestCases().get(0).getTime(),0);
        assertEquals(1,parser.currentTestSuite.getTests());
        assertEquals(1,parser.currentTestSuite.getFailures());
    }
}