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
import hudson.Functions;
import hudson.model.TaskListener;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ray
 */
public class JenkinsXCodeBuildOutputParser extends XCodeBuildOutputParser {
    protected TaskListener buildListener;
    private FilePath testReportsDir;
    private OutputStream logFileOutputStream;
    private boolean ignoreTestResults;

	public JenkinsXCodeBuildOutputParser(FilePath workspace, TaskListener buildListener) throws IOException, InterruptedException {
		super();
        this.buildListener = buildListener;
        this.captureOutputStream = new LineBasedFilterOutputStream();
        this.consoleLog = true;
        this.logFileOutputStream = null;
	this.ignoreTestResults = false;

        testReportsDir = workspace.child("test-reports");
        // Use the robust method to ensure the test reports directory is clean and ready.
        ensureDirectoryIsCleanAndExists(testReportsDir);
    }

    /**
     * Cleans and robustly recreates the specified directory.
     * This method handles existing directories, regular files, and broken symbolic links.
     * It ensures that after execution, the path exists and is an empty directory.
     *
     * @param dir The directory to clean and create.
     * @throws IOException If the cleanup or creation operation fails.
     * @throws InterruptedException If the operation is interrupted.
     */
    private void ensureDirectoryIsCleanAndExists(FilePath dir) throws IOException, InterruptedException {
        try {
            if (dir.exists()) {
                // Original behavior: recursively delete the directory to ensure it's empty.
                dir.deleteRecursive();
            }
        } catch (IOException e) {
            // This can fail on broken symbolic links. We'll try a direct delete.
            try {
                dir.delete();
            } catch (IOException ignored) {
                // Ignore and let mkdirs() throw the final exception if the path is still blocked.
            }
        }

        // If after all cleanup attempts, the path still exists but is not a directory,
        // which can happen with complex filesystem issues, perform one last delete.
        if (dir.exists() && !dir.isDirectory()) {
            dir.delete();
        }

        // Create the directory (and any parent directories if necessary).
        dir.mkdirs();
    }

    public void setConsoleLog(boolean consoleLog) {
        this.consoleLog = consoleLog;
    }

    public void setIgnoreTestResults(boolean ignoreTestResults) {
	this.ignoreTestResults = ignoreTestResults;
    }
    
    public void setLogfilePath(final FilePath buildDirectory, final String logfileOutputDirectory) throws IOException, InterruptedException {
	// Remove buildDirectory.exists() && buildDirectory.isDirectory() from condition.
	// Because If Generate archive is not specified, directory was not created.
        if(!StringUtils.isEmpty(logfileOutputDirectory)) {
	    // Fix not to use timestamp for log file name. (Use "xcodebuild.log" as a fixed file name)
	    // Because using a timestamp as a filename, No way to know it with a script etc.
            FilePath logFilePath = buildDirectory.child(logfileOutputDirectory);
            
            // Recursively clean the directory and ensure it exists,
            // without failing on broken symbolic links or stray files.
            ensureDirectoryIsCleanAndExists(logFilePath);

            logFileOutputStream = new BufferedOutputStream(logFilePath.child("xcodebuild.log").write(), 1024*512);
        }
    }
    
    public void closeLogfile() throws IOException {
    	if(logFileOutputStream != null) {
            logFileOutputStream.flush();
            logFileOutputStream.close();
            logFileOutputStream = null;
        }
    }

    public class LineBasedFilterOutputStream extends FilterOutputStream {
        StringBuilder buffer = new StringBuilder();

        public LineBasedFilterOutputStream() {
            super(buildListener.getLogger());
        }

        @Override
        public void write(int b) throws IOException {
	    if ( !ignoreTestResults ) {
                if((char)b == '\n') {
                    try {
                        handleLine(buffer.toString());
                        buffer = new StringBuilder();
                    } catch(Exception e) {  // Very fugly
                        Functions.printStackTrace(e, buildListener.fatalError(e.getMessage()));
                        throw new IOException(e);
                    }
                } else {
                    buffer.append((char)b);
                }
	    }
            if(consoleLog) {
                super.write(b);
            }
            if(logFileOutputStream != null) {
                logFileOutputStream.write(b);
            }
        }
        
        @Override
        public void close() throws IOException {
            if(logFileOutputStream != null) {
                logFileOutputStream.flush();
                logFileOutputStream.close();
                logFileOutputStream = null;
            }
            super.close();
        }
    }

	@Override
	protected OutputStream outputForSuite() throws IOException,
			InterruptedException {
		return testReportsDir.child("TEST-" + currentTestSuite.getName() + ".xml").write();
	}
}
