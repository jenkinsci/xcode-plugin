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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Installs Xcode by downloading and extracting a {@code .xip} archive on macOS nodes
 * using {@code /usr/bin/xip --expand}.
 */
public class XcodeXipInstaller extends ToolInstaller {

    static final String DEFAULT_SUBDIR = "Xcode.app/Contents/Developer";
    private static final ConcurrentMap<String, Object> NODE_FIRST_LAUNCH_LOCKS = new ConcurrentHashMap<>();

    private final String url;
    private final String subdir;
    private boolean runFirstLaunch;

    @DataBoundConstructor
    public XcodeXipInstaller(@CheckForNull String label, @NonNull String url, @CheckForNull String subdir) {
        super(label);
        this.url = Util.fixEmptyAndTrim(url);
        this.subdir = Util.fixEmptyAndTrim(subdir);
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @CheckForNull
    public String getSubdir() {
        return subdir;
    }

    public boolean isRunFirstLaunch() {
        return runFirstLaunch;
    }

    @DataBoundSetter
    public void setRunFirstLaunch(boolean runFirstLaunch) {
        this.runFirstLaunch = runFirstLaunch;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log)
            throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);
        dir.mkdirs();

        if (!isXipAvailable(node)) {
            throw new AbortException("Xcode .xip extraction is only supported on macOS nodes with /usr/bin/xip. "
                    + "Node '" + node.getDisplayName() + "' does not have /usr/bin/xip.");
        }

        String cacheMarker = url + (subdir != null ? "|" + subdir : "");
        FilePath markerFile = dir.child(".xip-installed");
        if (markerFile.exists()) {
            String installed = markerFile.readToString().trim();
            if (installed.equals(cacheMarker)) {
                FilePath resolved = resolveHome(dir);
                if (resolved.exists()) {
                    log.getLogger()
                            .println("[Xcode] Xcode already installed from " + url + " at " + resolved.getRemote());
                    ensureFirstLaunchIfEnabled(resolved, node, log);
                    return resolved;
                }
            }
        }

        log.getLogger().println("[Xcode] Installing Xcode from " + url + " on " + node.getDisplayName() + "...");

        FilePath localArchive = resolveAgentFilePath(url, node);
        FilePath archiveToExtract;
        boolean deleteArchiveAfterExtract = false;
        if (localArchive != null && localArchive.exists()) {
            log.getLogger()
                    .println("[Xcode] Using local .xip archive on " + node.getDisplayName() + ": "
                            + localArchive.getRemote());
            archiveToExtract = localArchive;
        } else {
            archiveToExtract = dir.child("xcode_installer.xip");
            deleteArchiveAfterExtract = true;
            downloadArchive(url, archiveToExtract, node, log);
        }

        log.getLogger().println("[Xcode] Extracting .xip archive using /usr/bin/xip --expand...");
        int exitCode = extractXip(archiveToExtract, dir, node, log);

        if (deleteArchiveAfterExtract) {
            archiveToExtract.delete();
        }
        if (exitCode != 0) {
            throw new AbortException("Failed to extract Xcode .xip archive: /usr/bin/xip exited with code " + exitCode);
        }

        FilePath resolved = resolveHome(dir);
        if (!resolved.exists()) {
            throw new AbortException("Extracted .xip archive, but expected Xcode developer directory was not found at "
                    + resolved.getRemote()
                    + ". Check the 'Subdirectory within archive' setting.");
        }

        markerFile.write(cacheMarker, "UTF-8");
        log.getLogger().println("[Xcode] Successfully installed Xcode at " + resolved.getRemote());
        ensureFirstLaunchIfEnabled(resolved, node, log);
        return resolved;
    }

    @CheckForNull
    protected FilePath resolveAgentFilePath(String downloadUrl, Node node) {
        if (downloadUrl == null || !downloadUrl.startsWith("file:")) {
            return null;
        }
        VirtualChannel channel = node.getChannel();
        if (channel == null) {
            return null;
        }
        try {
            URI uri = URI.create(downloadUrl);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    if (path.length() >= 3
                            && path.charAt(0) == '/'
                            && Character.isLetter(path.charAt(1))
                            && path.charAt(2) == ':') {
                        path = path.substring(1).replace('/', '\\');
                    }
                    return new FilePath(channel, path);
                }
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    protected boolean isXipAvailable(Node node) throws IOException, InterruptedException {
        VirtualChannel channel = node.getChannel();
        if (channel != null) {
            FilePath xipBinary = new FilePath(channel, "/usr/bin/xip");
            return xipBinary.exists();
        }
        return false;
    }

    protected ArgumentListBuilder getXipCommandLine(FilePath archive) {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("/usr/bin/xip", "--expand", archive.getRemote());
        return args;
    }

    protected int extractXip(FilePath archive, FilePath destinationDir, Node node, TaskListener log)
            throws IOException, InterruptedException {
        Launcher launcher = node.createLauncher(log);
        ArgumentListBuilder args = getXipCommandLine(archive);

        return launcher.launch()
                .cmds(args)
                .pwd(destinationDir)
                .stdout(log)
                .stderr(log.getLogger())
                .join();
    }

    /**
     * Checks license and first-launch initialization for {@code developerDir} and runs
     * {@code sudo -n /usr/bin/xcodebuild -license accept} / {@code -runFirstLaunch} when enabled and needed.
     */
    protected void ensureFirstLaunchIfEnabled(FilePath developerDir, Node node, TaskListener log)
            throws IOException, InterruptedException {
        if (!runFirstLaunch) {
            return;
        }
        String nodeKey = Util.fixNull(node.getNodeName());
        Object lock = NODE_FIRST_LAUNCH_LOCKS.computeIfAbsent(nodeKey, k -> new Object());
        synchronized (lock) {
            String devDir = developerDir.getRemote();
            if (checkFirstLaunchStatus(developerDir, node, log) == 0) {
                return;
            }

            if (checkLicenseStatus(developerDir, node, log) != 0) {
                log.getLogger()
                        .println("[Xcode] Running 'sudo -n /usr/bin/xcodebuild -license accept' for DEVELOPER_DIR="
                                + devDir);
                int licenseExit = executeSudoLicenseAccept(developerDir, node, log);
                if (licenseExit != 0) {
                    throw new AbortException("Failed to run 'sudo -n /usr/bin/xcodebuild -license accept' (exit code "
                            + licenseExit
                            + "). Ensure passwordless sudo is configured for '/usr/bin/xcodebuild -license accept, "
                            + "/usr/bin/xcodebuild -runFirstLaunch' with 'Defaults env_keep += \"DEVELOPER_DIR\"', "
                            + "or disable the first-launch sudo option.");
                }
            }

            log.getLogger()
                    .println("[Xcode] Running 'sudo -n /usr/bin/xcodebuild -runFirstLaunch' for DEVELOPER_DIR="
                            + devDir);
            int sudoExit = executeSudoRunFirstLaunch(developerDir, node, log);
            if (sudoExit != 0) {
                throw new AbortException("Failed to run 'sudo -n /usr/bin/xcodebuild -runFirstLaunch' (exit code "
                        + sudoExit
                        + "). Ensure passwordless sudo is configured for '/usr/bin/xcodebuild -license accept, "
                        + "/usr/bin/xcodebuild -runFirstLaunch' with 'Defaults env_keep += \"DEVELOPER_DIR\"', "
                        + "or disable the first-launch sudo option.");
            }

            int recheckExit = checkFirstLaunchStatus(developerDir, node, log);
            if (recheckExit != 0) {
                throw new AbortException(
                        "'sudo -n /usr/bin/xcodebuild -runFirstLaunch' exited 0, but 'xcodebuild -checkFirstLaunchStatus' "
                                + "still returned "
                                + recheckExit
                                + " for DEVELOPER_DIR="
                                + devDir
                                + ". Ensure sudoers preserves DEVELOPER_DIR (Defaults env_keep += \"DEVELOPER_DIR\").");
            }
            log.getLogger().println("[Xcode] Completed Xcode first-launch initialization for " + devDir);
        }
    }

    protected int checkFirstLaunchStatus(FilePath developerDir, Node node, TaskListener log)
            throws IOException, InterruptedException {
        return runWithDeveloperDir(
                developerDir,
                new ArgumentListBuilder().add("/usr/bin/xcodebuild", "-checkFirstLaunchStatus"),
                node,
                log);
    }

    protected int checkLicenseStatus(FilePath developerDir, Node node, TaskListener log)
            throws IOException, InterruptedException {
        return runWithDeveloperDir(
                developerDir, new ArgumentListBuilder().add("/usr/bin/xcodebuild", "-license", "check"), node, log);
    }

    protected int executeSudoLicenseAccept(FilePath developerDir, Node node, TaskListener log)
            throws IOException, InterruptedException {
        return runWithDeveloperDir(
                developerDir,
                new ArgumentListBuilder().add("/usr/bin/sudo", "-n", "/usr/bin/xcodebuild", "-license", "accept"),
                node,
                log);
    }

    protected int executeSudoRunFirstLaunch(FilePath developerDir, Node node, TaskListener log)
            throws IOException, InterruptedException {
        return runWithDeveloperDir(
                developerDir,
                new ArgumentListBuilder().add("/usr/bin/sudo", "-n", "/usr/bin/xcodebuild", "-runFirstLaunch"),
                node,
                log);
    }

    private int runWithDeveloperDir(FilePath developerDir, ArgumentListBuilder args, Node node, TaskListener log)
            throws IOException, InterruptedException {
        Launcher launcher = node.createLauncher(log);
        return launcher.launch()
                .cmds(args)
                .envs(Collections.singletonMap("DEVELOPER_DIR", developerDir.getRemote()))
                .stdout(log)
                .stderr(log.getLogger())
                .join();
    }

    protected ArgumentListBuilder getCurlCommandLine(String downloadUrl, FilePath targetFile) {
        ArgumentListBuilder curlArgs = new ArgumentListBuilder();
        curlArgs.add("/usr/bin/curl", "-f", "-s", "-S", "-L", "-o", targetFile.getRemote(), downloadUrl);
        return curlArgs;
    }

    protected void downloadArchive(String downloadUrl, FilePath targetFile, Node node, TaskListener log)
            throws IOException, InterruptedException {
        if ((downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) && isCurlAvailable(node)) {
            try {
                log.getLogger().println("[Xcode] Downloading .xip via curl on " + node.getDisplayName() + "...");
                Launcher launcher = node.createLauncher(log);
                ArgumentListBuilder curlArgs = getCurlCommandLine(downloadUrl, targetFile);
                int exit = launcher.launch()
                        .cmds(curlArgs)
                        .stdout(log)
                        .stderr(log.getLogger())
                        .join();
                if (exit == 0 && targetFile.exists() && targetFile.length() > 0) {
                    return;
                }
                log.getLogger()
                        .println("[Xcode] Direct curl download failed (exit code " + exit
                                + "), streaming through controller...");
            } catch (IOException e) {
                log.getLogger()
                        .println("[Xcode] Direct curl download failed (" + e.getMessage()
                                + "), streaming through controller...");
            }
        }

        log.getLogger().println("[Xcode] Streaming .xip from " + downloadUrl + " to agent...");
        URL u = URI.create(downloadUrl).toURL();
        URLConnection conn = ProxyConfiguration.open(u);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        try (InputStream in = conn.getInputStream()) {
            targetFile.copyFrom(in);
        }
    }

    protected boolean isCurlAvailable(Node node) throws IOException, InterruptedException {
        VirtualChannel channel = node.getChannel();
        if (channel == null) {
            return false;
        }
        FilePath curl = new FilePath(channel, "/usr/bin/curl");
        return curl.exists();
    }

    public FilePath resolveHome(FilePath dir) throws IOException, InterruptedException {
        if (subdir != null) {
            FilePath explicit = dir.child(subdir);
            if (explicit.exists() || !DEFAULT_SUBDIR.equals(subdir)) {
                return explicit;
            }
            FilePath detected = findAppDeveloperDir(dir);
            return detected != null ? detected : explicit;
        }
        FilePath detected = findAppDeveloperDir(dir);
        if (detected != null) {
            return detected;
        }
        FilePath contentsDeveloper = dir.child("Contents/Developer");
        if (contentsDeveloper.exists()) {
            return contentsDeveloper;
        }
        return dir;
    }

    @CheckForNull
    private FilePath findAppDeveloperDir(FilePath dir) throws IOException, InterruptedException {
        FilePath xcodeAppDeveloper = dir.child(DEFAULT_SUBDIR);
        if (xcodeAppDeveloper.exists()) {
            return xcodeAppDeveloper;
        }
        FilePath betaDeveloper = dir.child("Xcode-beta.app/Contents/Developer");
        if (betaDeveloper.exists()) {
            return betaDeveloper;
        }
        if (dir.exists()) {
            List<FilePath> subdirs = dir.listDirectories();
            if (subdirs != null) {
                for (FilePath candidate : subdirs) {
                    if (candidate.getName().endsWith(".app")) {
                        FilePath devDir = candidate.child("Contents/Developer");
                        if (devDir.exists()) {
                            return devDir;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Extension
    @Symbol("xcodeXip")
    public static class DescriptorImpl extends ToolInstallerDescriptor<XcodeXipInstaller> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Extract .xip archive";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == XcodeInstallation.class;
        }

        @RequirePOST
        public FormValidation doCheckUrl(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            value = Util.fixEmptyAndTrim(value);
            if (value == null) {
                return FormValidation.error("URL is required.");
            }
            try {
                URI uri = new URI(value);
                if (uri.getScheme() == null) {
                    return FormValidation.error("URL must specify a scheme (e.g. https:// or file://).");
                }
            } catch (URISyntaxException e) {
                return FormValidation.error("Invalid URL: " + e.getMessage());
            }
            return FormValidation.ok();
        }
    }
}
