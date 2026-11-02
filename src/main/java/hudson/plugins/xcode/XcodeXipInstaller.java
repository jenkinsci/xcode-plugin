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
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Installs Xcode by downloading and extracting a {@code .xip} archive on macOS nodes
 * using {@code /usr/bin/xip --expand}.
 */
public class XcodeXipInstaller extends ToolInstaller {

    private final String url;
    private final String subdir;

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

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log)
            throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);
        dir.mkdirs();

        if (!isXipAvailable(node)) {
            throw new AbortException(
                    "Xcode .xip extraction is only supported on macOS nodes with /usr/bin/xip. "
                            + "Node '" + node.getDisplayName() + "' does not have /usr/bin/xip.");
        }

        String cacheMarker = url + (subdir != null ? "|" + subdir : "");
        FilePath markerFile = dir.child(".xip-installed");
        if (markerFile.exists()) {
            String installed = markerFile.readToString().trim();
            if (installed.equals(cacheMarker)) {
                FilePath resolved = resolveHome(dir);
                if (resolved.exists()) {
                    log.getLogger().println("[Xcode] Xcode already installed from " + url + " at " + resolved.getRemote());
                    return resolved;
                }
            }
        }

        log.getLogger().println("[Xcode] Installing Xcode from " + url + " on " + node.getDisplayName() + "...");

        FilePath tempArchive = dir.child("xcode_installer.xip");
        downloadArchive(url, tempArchive, node, log);

        log.getLogger().println("[Xcode] Extracting .xip archive using /usr/bin/xip --expand...");
        int exitCode = extractXip(tempArchive, dir, node, log);

        if (exitCode != 0) {
            tempArchive.delete();
            throw new AbortException("Failed to extract Xcode .xip archive: /usr/bin/xip exited with code " + exitCode);
        }

        tempArchive.delete();
        markerFile.write(cacheMarker, "UTF-8");

        FilePath resolved = resolveHome(dir);
        log.getLogger().println("[Xcode] Successfully installed Xcode at " + resolved.getRemote());
        return resolved;
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
                int exit = launcher.launch().cmds(curlArgs).stdout(log).stderr(log.getLogger()).join();
                if (exit == 0 && targetFile.exists() && targetFile.length() > 0) {
                    return;
                }
                log.getLogger().println("[Xcode] Direct curl download failed (exit code " + exit + "), streaming through controller...");
            } catch (IOException e) {
                log.getLogger().println("[Xcode] Direct curl download failed (" + e.getMessage() + "), streaming through controller...");
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
            return dir.child(subdir);
        }
        FilePath xcodeAppDeveloper = dir.child("Xcode.app/Contents/Developer");
        if (xcodeAppDeveloper.exists()) {
            return xcodeAppDeveloper;
        }
        FilePath contentsDeveloper = dir.child("Contents/Developer");
        if (contentsDeveloper.exists()) {
            return contentsDeveloper;
        }
        return dir;
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
