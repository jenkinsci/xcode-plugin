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

import hudson.FilePath;
import hudson.console.ConsoleNote;
import hudson.model.TaskListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ray
 */
public class HudsonXCodeBuildOutputParserTest {
    OutputParserTests test;

    @Before
    public void setUp() throws IOException, InterruptedException {
        HudsonXCodeBuildOutputParser parser = new HudsonXCodeBuildOutputParser(new FilePath(new File(".")), new TaskListener() {

            public PrintStream getLogger() {
                try {
                    return new PrintStream("test-output.txt");
                } catch(FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            public void annotate(ConsoleNote consoleNote) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void hyperlink(String s, String s1) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public PrintWriter error(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public PrintWriter error(String string, Object... os) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public PrintWriter fatalError(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public PrintWriter fatalError(String string, Object... os) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        test = new OutputParserTests(parser);
    }

    @After
    public void tearDown() {
    }
    @Test
    public void shouldIgnoreStartSuiteLineThatContainsFullPath() throws Exception {
    	test.shouldIgnoreStartSuiteLineThatContainsFullPath();
    }

    @Test
    public void shouldParseStartTestSuite() throws Exception {
    	test.shouldParseStartTestSuite();
    }

    @Test
    public void shouldParseEndTestSuite() throws Exception {
    	test.shouldParseEndTestSuite();
    }

    @Test
    public void shouldParseStartTestCase() throws Exception {
    	test.shouldParseStartTestCase();
    }

    @Test
    public void shouldAddErrorToTestCase() throws Exception {
    	test.shouldAddErrorToTestCase();
    }

    @Test
    public void shouldParsePassedTestCase() throws Exception {
    	test.shouldParsePassedTestCase();
    }

    @Test
    public void shouldParseFailedTestCase() throws Exception {
    	test.shouldParseFailedTestCase();
    }
}