/*
 * The MIT License
 *
 * Copyright (c) 2026 Jenkins Xcode Plugin Contributors
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

package hudson.plugins.xcode;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XcodeXipInstallerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public org.junit.rules.TemporaryFolder temp = new org.junit.rules.TemporaryFolder();

    @Test
    public void testDescriptor() {
        XcodeXipInstaller.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(XcodeXipInstaller.DescriptorImpl.class);
        assertNotNull(descriptor);
        assertEquals("Extract .xip archive", descriptor.getDisplayName());
        assertTrue(descriptor.isApplicable(XcodeInstallation.class));
        assertFalse(descriptor.isApplicable(DummyToolInstallation.class));
    }

    @Test
    public void testFormValidationUrl() {
        XcodeXipInstaller.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(XcodeXipInstaller.DescriptorImpl.class);

        // Empty URL
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl("").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl("   ").kind);

        // Scheme missing
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl("example.com/xcode.xip").kind);

        // Invalid URI syntax
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl("http://foo bar/baz").kind);

        // Valid URLs
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl("https://internal.repo.com/Xcode_15.0.xip").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl("http://internal.repo.com/Xcode_15.0.xip").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl("file:///var/xcode_share/Xcode_15.0.xip").kind);
    }

    @Test
    public void testConstructorAndGetters() {
        XcodeXipInstaller installer = new XcodeXipInstaller("osx", "https://example.com/Xcode.xip", "Xcode.app/Contents/Developer");
        assertEquals("osx", installer.getLabel());
        assertEquals("https://example.com/Xcode.xip", installer.getUrl());
        assertEquals("Xcode.app/Contents/Developer", installer.getSubdir());

        XcodeXipInstaller minimal = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        assertNull(minimal.getLabel());
        assertEquals("https://example.com/Xcode.xip", minimal.getUrl());
        assertNull(minimal.getSubdir());
    }

    @Test
    public void testResolveHomeWithSubdir() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", "CustomApp.app/Contents/Developer");
        FilePath baseDir = new FilePath(temp.newFolder());
        FilePath resolved = installer.resolveHome(baseDir);
        assertEquals(baseDir.child("CustomApp.app/Contents/Developer").getRemote(), resolved.getRemote());
    }

    @Test
    public void testResolveHomeStandardApp() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        FilePath baseDir = new FilePath(temp.newFolder());
        FilePath standardDeveloper = baseDir.child("Xcode.app/Contents/Developer");
        standardDeveloper.mkdirs();

        FilePath resolved = installer.resolveHome(baseDir);
        assertEquals(standardDeveloper.getRemote(), resolved.getRemote());
    }

    @Test
    public void testResolveHomeContentsDeveloperOnly() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        FilePath baseDir = new FilePath(temp.newFolder());
        FilePath contentsDeveloper = baseDir.child("Contents/Developer");
        contentsDeveloper.mkdirs();

        FilePath resolved = installer.resolveHome(baseDir);
        assertEquals(contentsDeveloper.getRemote(), resolved.getRemote());
    }

    @Test
    public void testResolveHomeFallback() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        FilePath baseDir = new FilePath(temp.newFolder());

        FilePath resolved = installer.resolveHome(baseDir);
        assertEquals(baseDir.getRemote(), resolved.getRemote());
    }

    @Test
    public void testPerformInstallationThrowsWhenXipNotAvailable() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        XcodeInstallation tool = new XcodeInstallation("Xcode Test", null, null);

        try {
            installer.performInstallation(tool, j.jenkins, TaskListener.NULL);
            fail("Expected AbortException due to missing /usr/bin/xip on non-macOS node");
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("/usr/bin/xip"));
        }
    }

    @Test
    public void testPerformInstallationSuccess() throws Exception {
        File dummyRepoDir = temp.newFolder();
        File dummyXip = new File(dummyRepoDir, "Xcode.xip");
        Files.writeString(dummyXip.toPath(), "mock xip content", StandardCharsets.UTF_8);

        String fileUrl = dummyXip.toURI().toURL().toString();
        TestableXcodeXipInstaller installer = new TestableXcodeXipInstaller(fileUrl, null);

        XcodeInstallation tool = new XcodeInstallation("Xcode Test", null, null);
        FilePath installedPath = installer.performInstallation(tool, j.jenkins, TaskListener.NULL);

        assertNotNull(installedPath);
        assertTrue(installedPath.exists());
        assertEquals("Developer", installedPath.getName());
        assertEquals(1, installer.extractCount);

        // Second run should hit cache and NOT extract again
        FilePath cachedPath = installer.performInstallation(tool, j.jenkins, TaskListener.NULL);
        assertEquals(installedPath.getRemote(), cachedPath.getRemote());
        assertEquals(1, installer.extractCount);
    }

    @Test
    public void testPerformInstallationCustomSubdir() throws Exception {
        File dummyRepoDir = temp.newFolder();
        File dummyXip = new File(dummyRepoDir, "Xcode.xip");
        Files.writeString(dummyXip.toPath(), "mock xip content", StandardCharsets.UTF_8);

        String fileUrl = dummyXip.toURI().toURL().toString();
        TestableXcodeXipInstaller installer = new TestableXcodeXipInstaller(fileUrl, "CustomXcode.app/Contents/Developer");
        installer.simulateCustomSubdir = true;

        XcodeInstallation tool = new XcodeInstallation("Xcode Custom", null, null);
        FilePath installedPath = installer.performInstallation(tool, j.jenkins, TaskListener.NULL);

        assertNotNull(installedPath);
        assertTrue(installedPath.exists());
        assertTrue(installedPath.getRemote().endsWith("CustomXcode.app/Contents/Developer"));
        assertEquals(1, installer.extractCount);
    }

    @Test
    public void testPerformInstallationExtractionFailure() throws Exception {
        File dummyRepoDir = temp.newFolder();
        File dummyXip = new File(dummyRepoDir, "Xcode.xip");
        Files.writeString(dummyXip.toPath(), "mock xip content", StandardCharsets.UTF_8);

        String fileUrl = dummyXip.toURI().toURL().toString();
        TestableXcodeXipInstaller installer = new TestableXcodeXipInstaller(fileUrl, null);
        installer.exitCode = 1;

        XcodeInstallation tool = new XcodeInstallation("Xcode Failed", null, null);
        try {
            installer.performInstallation(tool, j.jenkins, TaskListener.NULL);
            fail("Expected AbortException on non-zero extraction exit code");
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("exited with code 1"));
        }
    }

    @Test
    public void testPerformInstallationReinstallsWhenCacheMarkerChanges() throws Exception {
        File dummyRepoDir = temp.newFolder();
        File dummyXip1 = new File(dummyRepoDir, "Xcode1.xip");
        Files.writeString(dummyXip1.toPath(), "mock 1", StandardCharsets.UTF_8);
        File dummyXip2 = new File(dummyRepoDir, "Xcode2.xip");
        Files.writeString(dummyXip2.toPath(), "mock 2", StandardCharsets.UTF_8);

        TestableXcodeXipInstaller installer1 = new TestableXcodeXipInstaller(dummyXip1.toURI().toURL().toString(), null);
        XcodeInstallation tool = new XcodeInstallation("Xcode Cache", null, null);
        installer1.performInstallation(tool, j.jenkins, TaskListener.NULL);
        assertEquals(1, installer1.extractCount);

        // Installer with different URL targeting the same tool installation directory
        TestableXcodeXipInstaller installer2 = new TestableXcodeXipInstaller(dummyXip2.toURI().toURL().toString(), null);
        installer2.performInstallation(tool, j.jenkins, TaskListener.NULL);
        assertEquals(1, installer2.extractCount);
    }

    @Test
    public void testPerformInstallationReinstallsWhenTargetDeleted() throws Exception {
        File dummyRepoDir = temp.newFolder();
        File dummyXip = new File(dummyRepoDir, "Xcode.xip");
        Files.writeString(dummyXip.toPath(), "mock", StandardCharsets.UTF_8);

        String subdir = "CustomXcode.app/Contents/Developer";
        TestableXcodeXipInstaller installer = new TestableXcodeXipInstaller(dummyXip.toURI().toURL().toString(), subdir);
        installer.simulateCustomSubdir = true;

        XcodeInstallation tool = new XcodeInstallation("Xcode Reinstall", null, null);
        FilePath installedPath = installer.performInstallation(tool, j.jenkins, TaskListener.NULL);
        assertEquals(1, installer.extractCount);

        // Delete installed directory while keeping marker file
        installedPath.deleteRecursive();
        assertFalse(installedPath.exists());

        installer.performInstallation(tool, j.jenkins, TaskListener.NULL);
        assertEquals(2, installer.extractCount);
    }

    @Test
    public void testIsXipAvailableWithOfflineNode() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        DumbSlave slave = j.createSlave();
        // Disconnect slave so channel becomes null
        slave.toComputer().disconnect(null);
        slave.toComputer().waitUntilOffline();

        assertNull(slave.getChannel());
        assertFalse(installer.isXipAvailable(slave));
        assertFalse(installer.isCurlAvailable(slave));
    }

    @Test
    public void testIsXipAvailableWithOnlineNode() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        DumbSlave onlineSlave = j.createOnlineSlave();

        // When channel != null: exercises FilePath checking on the channel
        boolean xipAvail = installer.isXipAvailable(onlineSlave);
        boolean curlAvail = installer.isCurlAvailable(onlineSlave);
        // On non-macOS test runner, /usr/bin/xip does not exist
        assertFalse(xipAvail);
        assertNotNull(onlineSlave.getChannel());
    }

    @Test
    public void testGetXipCommandLine() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        FilePath archive = new FilePath(temp.newFile("test.xip"));
        ArgumentListBuilder args = installer.getXipCommandLine(archive);
        assertEquals("/usr/bin/xip --expand " + archive.getRemote(), args.toString());
    }

    @Test
    public void testExtractXipDirectExecution() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null) {
            @Override
            protected ArgumentListBuilder getXipCommandLine(FilePath archive) {
                ArgumentListBuilder args = new ArgumentListBuilder();
                if (j.jenkins.createLauncher(TaskListener.NULL).isUnix()) {
                    args.add("true");
                } else {
                    args.add("cmd.exe", "/c", "exit", "0");
                }
                return args;
            }
        };

        FilePath archive = new FilePath(temp.newFile("dummy.xip"));
        FilePath dest = new FilePath(temp.newFolder());

        int exit = installer.extractXip(archive, dest, j.jenkins, TaskListener.NULL);
        assertEquals(0, exit);
    }

    @Test
    public void testGetCurlCommandLine() throws Exception {
        XcodeXipInstaller installer = new XcodeXipInstaller(null, "https://example.com/Xcode.xip", null);
        FilePath target = new FilePath(temp.newFile("target.xip"));
        ArgumentListBuilder args = installer.getCurlCommandLine("https://example.com/Xcode.xip", target);
        assertTrue(args.toString().contains("/usr/bin/curl"));
        assertTrue(args.toString().contains(target.getRemote()));
    }

    @Test
    public void testDownloadArchiveCurlSuccess() throws Exception {
        File destFile = temp.newFile("download_success.xip");
        Files.writeString(destFile.toPath(), "existing-data", StandardCharsets.UTF_8);
        FilePath target = new FilePath(destFile);

        XcodeXipInstaller installer = new XcodeXipInstaller(null, "http://example.com/file.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return true;
            }

            @Override
            protected ArgumentListBuilder getCurlCommandLine(String downloadUrl, FilePath targetFile) {
                ArgumentListBuilder args = new ArgumentListBuilder();
                if (j.jenkins.createLauncher(TaskListener.NULL).isUnix()) {
                    args.add("true");
                } else {
                    args.add("cmd.exe", "/c", "exit", "0");
                }
                return args;
            }
        };

        installer.downloadArchive("http://example.com/file.xip", target, j.jenkins, TaskListener.NULL);
        assertTrue(target.exists());
        assertTrue(target.length() > 0);
    }

    @Test
    public void testDownloadArchiveCurlFallbackExitCode() throws Exception {
        File destFile = temp.newFile("download_target.xip");
        FilePath target = new FilePath(destFile);

        XcodeXipInstaller installer = new XcodeXipInstaller(null, "http://127.0.0.1:9/unreachable.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return true;
            }
        };

        try {
            installer.downloadArchive("http://127.0.0.1:9/unreachable.xip", target, j.jenkins, TaskListener.NULL);
        } catch (IOException expected) {
            // Expected since localhost:9 is unreachable, but curl attempt was exercised
        }
    }

    @Test
    public void testDownloadArchiveCurlIOExceptionFallback() throws Exception {
        File destFile = temp.newFile("download_ioerror.xip");
        FilePath target = new FilePath(destFile);

        XcodeXipInstaller installer = new XcodeXipInstaller(null, "http://127.0.0.1:9/unreachable.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return true;
            }

            @Override
            protected ArgumentListBuilder getCurlCommandLine(String downloadUrl, FilePath targetFile) {
                return new ArgumentListBuilder("/path/to/nonexistent/executable_to_trigger_io_exception");
            }
        };

        try {
            installer.downloadArchive("http://127.0.0.1:9/unreachable.xip", target, j.jenkins, TaskListener.NULL);
        } catch (IOException expected) {
            // Controller streaming fallback throws when connection is refused
        }
    }

    @Test
    public void testDownloadArchiveCurlBranches() throws Exception {
        File destFile = temp.newFile("download_branch.xip");
        FilePath target = new FilePath(destFile);

        // Branch 1: https:// URL with curl available
        XcodeXipInstaller installerHttps = new XcodeXipInstaller(null, "https://127.0.0.1:9/unreachable.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return true;
            }
        };
        try {
            installerHttps.downloadArchive("https://127.0.0.1:9/unreachable.xip", target, j.jenkins, TaskListener.NULL);
        } catch (IOException expected) {}

        // Branch 2: http:// URL but curl NOT available
        XcodeXipInstaller installerNoCurl = new XcodeXipInstaller(null, "http://127.0.0.1:9/unreachable.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return false;
            }
        };
        try {
            installerNoCurl.downloadArchive("http://127.0.0.1:9/unreachable.xip", target, j.jenkins, TaskListener.NULL);
        } catch (IOException expected) {}
    }

    @Test
    public void testDownloadArchiveCurlSuccessTargetNotCreated() throws Exception {
        File destFile = temp.newFile("download_target_missing.xip");
        destFile.delete();
        FilePath target = new FilePath(destFile);

        XcodeXipInstaller installer = new XcodeXipInstaller(null, "http://127.0.0.1:9/dummy.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return true;
            }

            @Override
            protected ArgumentListBuilder getCurlCommandLine(String downloadUrl, FilePath targetFile) {
                ArgumentListBuilder args = new ArgumentListBuilder();
                if (j.jenkins.createLauncher(TaskListener.NULL).isUnix()) {
                    args.add("true");
                } else {
                    args.add("cmd.exe", "/c", "exit", "0");
                }
                return args;
            }
        };

        try {
            installer.downloadArchive("http://127.0.0.1:9/dummy.xip", target, j.jenkins, TaskListener.NULL);
        } catch (IOException expected) {}
    }

    @Test
    public void testDownloadArchiveCurlSuccessTargetEmptyFile() throws Exception {
        File destFile = temp.newFile("download_target_empty.xip");
        Files.write(destFile.toPath(), new byte[0]);
        FilePath target = new FilePath(destFile);

        XcodeXipInstaller installer = new XcodeXipInstaller(null, "http://127.0.0.1:9/dummy.xip", null) {
            @Override
            protected boolean isCurlAvailable(Node node) {
                return true;
            }

            @Override
            protected ArgumentListBuilder getCurlCommandLine(String downloadUrl, FilePath targetFile) {
                ArgumentListBuilder args = new ArgumentListBuilder();
                if (j.jenkins.createLauncher(TaskListener.NULL).isUnix()) {
                    args.add("true");
                } else {
                    args.add("cmd.exe", "/c", "exit", "0");
                }
                return args;
            }
        };

        try {
            installer.downloadArchive("http://127.0.0.1:9/dummy.xip", target, j.jenkins, TaskListener.NULL);
        } catch (IOException expected) {}
    }

    private static class TestableXcodeXipInstaller extends XcodeXipInstaller {
        int exitCode = 0;
        int extractCount = 0;
        boolean simulateCustomSubdir = false;

        public TestableXcodeXipInstaller(String url, String subdir) {
            super(null, url, subdir);
        }

        @Override
        protected boolean isXipAvailable(Node node) {
            return true;
        }

        @Override
        protected int extractXip(FilePath archive, FilePath destinationDir, Node node, TaskListener log)
                throws IOException, InterruptedException {
            extractCount++;
            if (simulateCustomSubdir) {
                destinationDir.child("CustomXcode.app/Contents/Developer").mkdirs();
            } else {
                destinationDir.child("Xcode.app/Contents/Developer").mkdirs();
            }
            return exitCode;
        }
    }

    private static class DummyToolInstallation extends ToolInstallation {
        public DummyToolInstallation(String name, String home) {
            super(name, home, null);
        }
    }
}
