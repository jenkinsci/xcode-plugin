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
import hudson.model.TaskListener;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author ray
 */
public class HudsonXCodeBuildOutputParser extends XCodeBuildOutputParser {
    protected TaskListener buildListener;
    private FilePath testReportsDir;

	public HudsonXCodeBuildOutputParser(FilePath workspace, TaskListener buildListener) throws IOException, InterruptedException {
		super();
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

	@Override
	protected OutputStream outputForSuite() throws IOException,
			InterruptedException {
		return testReportsDir.child("TEST-" + currentTestSuite.getName() + ".xml").write();
	}
}
