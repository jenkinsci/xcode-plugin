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

import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ray Hilton
 */
public class XCodeBuilder extends Builder {
    public final Boolean cleanBeforeBuild;
    public final String configuration;
    public final String target;
    public final String sdk;
    public final String xcodeProjectPath;
    public final String xcodeProjectFile;
    public final String xcodeSchema;
    public final String xcodeWorkspaceFile;
    public final String embeddedProfileFile;
    public final String cfBundleVersionValue;
    public final String cfBundleShortVersionStringValue;
    public final Boolean buildIpa;
    public final Boolean unlockKeychain;
    public final String keychainPath;
    public final String keychainPwd;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public XCodeBuilder(Boolean buildIpa, Boolean cleanBeforeBuild, String configuration, String target, String sdk, String xcodeProjectPath, String xcodeProjectFile, String xcodeWorkspaceFile, String xcodeSchema, String embeddedProfileFile, String cfBundleVersionValue, String cfBundleShortVersionStringValue, Boolean unlockKeychain, String keychainPath, String keychainPwd) {
        this.buildIpa = buildIpa;
        this.sdk = sdk;
        this.target = target;
        this.cleanBeforeBuild = cleanBeforeBuild;
        this.configuration = configuration;
        this.xcodeProjectPath = xcodeProjectPath;
        this.xcodeProjectFile = xcodeProjectFile;
        this.xcodeWorkspaceFile = xcodeWorkspaceFile;
        this.xcodeSchema = xcodeSchema;
        this.embeddedProfileFile = embeddedProfileFile;
        this.cfBundleVersionValue = cfBundleVersionValue;
        this.cfBundleShortVersionStringValue = cfBundleShortVersionStringValue;
        this.unlockKeychain = unlockKeychain;
        this.keychainPath = keychainPath;
        this.keychainPwd = keychainPwd;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars envs = build.getEnvironment(listener);
        FilePath projectRoot = build.getWorkspace();

        // check that the configured tools exist
        if (!new FilePath(projectRoot.getChannel(), getDescriptor().getXcodebuildPath()).exists()) {
            listener.fatalError(Messages.XCodeBuilder_xcodebuildNotFound(getDescriptor().getXcodebuildPath()));
            return false;
        }
        if (!new FilePath(projectRoot.getChannel(), getDescriptor().getAgvtoolPath()).exists()) {
            listener.fatalError(Messages.XCodeBuilder_avgtoolNotFound(getDescriptor().getAgvtoolPath()));
            return false;
        }

        // Set the working directory
        if (!StringUtils.isEmpty(xcodeProjectPath)) {
            projectRoot = projectRoot.child(xcodeProjectPath);
        }
        listener.getLogger().println(Messages.XCodeBuilder_workingDir(projectRoot));
        FilePath buildDirectory = projectRoot.child("build").child(configuration + "-iphoneos");

        // XCode Version
        int returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getXcodebuildPath(), "-version").stdout(listener).pwd(projectRoot).join();
        if (returnCode > 0) {
            listener.fatalError(Messages.XCodeBuilder_xcodeVersionNotFound());
            return false; // We fail the build if XCode isn't deployed
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Try to read CFBundleShortVersionString from project
        listener.getLogger().println(Messages.XCodeBuilder_fetchingCFBundleShortVersionString());
        String cfBundleShortVersionString = "";
        returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getAgvtoolPath(), "mvers", "-terse1").stdout(output).pwd(projectRoot).join();
        // only use this version number if we found it
        if (returnCode == 0)
            cfBundleShortVersionString = output.toString().trim();
        if (cfBundleShortVersionString.isEmpty())
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringNotFound());
        else
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringFound(cfBundleShortVersionString));

        // Try to read CFBundleVersion from project
        listener.getLogger().println(Messages.XCodeBuilder_fetchingCFBundleVersion());
        String cfBundleVersion = "";
        returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getAgvtoolPath(), "vers", "-terse").stdout(output).pwd(projectRoot).join();
        // only use this version number if we found it
        if (returnCode == 0)
            cfBundleVersion = output.toString().trim();
        if (cfBundleVersion.isEmpty())
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionNotFound());
        else
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionFound(cfBundleShortVersionString));

        listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringValue(cfBundleShortVersionString));
        listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionValue(cfBundleVersion));

        // Update the Marketing version (CFBundleShortVersionString)
        if (!StringUtils.isEmpty(cfBundleShortVersionStringValue)) {
            try {
                // If not empty we use the Token Expansion to replace it
                // https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin
                cfBundleShortVersionString = TokenMacro.expand(build, listener, cfBundleShortVersionStringValue);
                listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringUpdate(cfBundleShortVersionString));
                returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getAgvtoolPath(), "new-marketing-version", cfBundleShortVersionString).stdout(listener).pwd(projectRoot).join();
                if (returnCode > 0) {
                    listener.fatalError(Messages.XCodeBuilder_CFBundleShortVersionStringUpdateError(cfBundleShortVersionString));
                    return false;
                }
            } catch (MacroEvaluationException e) {
                listener.fatalError(Messages.XCodeBuilder_CFBundleShortVersionStringMacroError(e.getMessage()));
                return false;
            }
        }

        // Update the Technical version (CFBundleVersion)
        if (!StringUtils.isEmpty(cfBundleVersionValue)) {
            try {
                // If not empty we use the Token Expansion to replace it
                // https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin
                cfBundleVersion = TokenMacro.expand(build, listener, cfBundleVersionValue);
                listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionUpdate(cfBundleVersion));
                returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getAgvtoolPath(), "new-version", "-all", cfBundleVersion).stdout(listener).pwd(projectRoot).join();
                if (returnCode > 0) {
                    listener.fatalError(Messages.XCodeBuilder_CFBundleVersionUpdateError(cfBundleVersion));
                    return false;
                }
            } catch (MacroEvaluationException e) {
                listener.fatalError(Messages.XCodeBuilder_CFBundleVersionMacroError(e.getMessage()));
                // Fails the build
                return false;
            }
        }

        listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringUsed(cfBundleShortVersionString));
        listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionUsed(cfBundleVersion));

        // Clean build directories
        if (cleanBeforeBuild) {
            listener.getLogger().println(Messages.XCodeBuilder_cleaningBuildDir(projectRoot.child("build")));
            buildDirectory.deleteRecursive();
        }

        // remove test-reports and *.ipa
        listener.getLogger().println(Messages.XCodeBuilder_cleaningTestReportsDir());
        projectRoot.child("test-reports").deleteRecursive();

        if (unlockKeychain) {
            // Let's unlock the keychain
            launcher.launch().envs(envs).cmds("/usr/bin/security","list-keychains","-s",keychainPath).stdout(listener).pwd(projectRoot).join();
            launcher.launch().envs(envs).cmds("/usr/bin/security","login-keychain","-d","user","-s",keychainPath).stdout(listener).pwd(projectRoot).join();
            if(StringUtils.isEmpty(keychainPwd))
              returnCode = launcher.launch().envs(envs).cmds("/usr/bin/security","unlock-keychain",keychainPath).stdout(listener).pwd(projectRoot).join();
            else
              returnCode = launcher.launch().envs(envs).cmds("/usr/bin/security","unlock-keychain","-p",keychainPwd,keychainPath).masks(false,false,false,true,false).stdout(listener).pwd(projectRoot).join();
            if (returnCode > 0) {
                listener.fatalError(Messages.XCodeBuilder_unlockKeychainFailed());
                return false;
            }
        }

        // Build
        StringBuilder xcodeReport = new StringBuilder(Messages.XCodeBuilder_invokeXcodebuild());
        XCodeBuildOutputParser reportGenerator = new XCodeBuildOutputParser(projectRoot, listener);
        List<String> commandLine = Lists.newArrayList(getDescriptor().getXcodebuildPath());

        // Priritizing schemaa over target setting
        if(!StringUtils.isEmpty(xcodeSchema)) {
            commandLine.add("-scheme");
            commandLine.add(xcodeSchema);
            xcodeReport.append(", scheme: ").append(xcodeSchema);
        } else if (StringUtils.isEmpty(target)) {
            commandLine.add("-alltargets");
            xcodeReport.append("target: ALL");
        } else {
            commandLine.add("-target");
            commandLine.add(target);
            xcodeReport.append("target: ").append(target);
        }

        if (!StringUtils.isEmpty(sdk)) {
            commandLine.add("-sdk");
            commandLine.add(sdk);
            xcodeReport.append(", sdk: ").append(sdk);
        } else {
            xcodeReport.append(", sdk: DEFAULT");
        }

        // Prioritizing workspace over project setting
        if (!StringUtils.isEmpty(xcodeWorkspaceFile)) {
            commandLine.add("-workspace");
            commandLine.add(xcodeWorkspaceFile + ".xcworkspace");
            xcodeReport.append(", workspace: ").append(xcodeWorkspaceFile);
        } else if (!StringUtils.isEmpty(xcodeProjectFile)) {
            commandLine.add("-project");
            commandLine.add(xcodeProjectFile);
            xcodeReport.append(", project: ").append(xcodeProjectFile);
        } else {
            xcodeReport.append(", project: DEFAULT");
        }

        commandLine.add("-configuration");
        commandLine.add(configuration);
        xcodeReport.append(", configuration: ").append(configuration);

        if (cleanBeforeBuild) {
            commandLine.add("clean");
            xcodeReport.append(", clean: YES");
        } else {
            xcodeReport.append(", clean: NO");
        }
        commandLine.add("build");

        listener.getLogger().println(xcodeReport.toString());
        returnCode = launcher.launch().envs(envs).cmds(commandLine).stdout(reportGenerator.getOutputStream()).pwd(projectRoot).join();
        if (reportGenerator.getExitCode() != 0) return false;
        if (returnCode > 0) return false;


        // Package IPA
        if (buildIpa) {

            listener.getLogger().println(Messages.XCodeBuilder_cleaningIPA());
            for (FilePath path : buildDirectory.list("*.ipa")) {
                path.delete();
            }

            listener.getLogger().println(Messages.XCodeBuilder_packagingIPA());
            List<FilePath> apps = buildDirectory.list(new AppFileFilter());

            for (FilePath app : apps) {
                String version = "";
                if (cfBundleShortVersionString.isEmpty() && cfBundleVersion.isEmpty())
                    version = "" + build.getNumber();
                else if (cfBundleVersion.isEmpty())
                    version = cfBundleShortVersionString;
                else
                    version = cfBundleVersion;

                String baseName = app.getBaseName().replaceAll(" ", "_") + "-" +
                        configuration.replaceAll(" ", "_") + (version.isEmpty() ? "" : "-" + version);

                FilePath ipaLocation = buildDirectory.child(baseName + ".ipa");

                FilePath payload = buildDirectory.child("Payload");
                payload.deleteRecursive();
                payload.mkdirs();


                listener.getLogger().println("Packaging " + app.getBaseName() + ".app => " + ipaLocation);
                List<String> packageCommandLine = new ArrayList<String>();
                packageCommandLine.add(getDescriptor().getXcrunPath());
                packageCommandLine.add("-sdk");

                if (!StringUtils.isEmpty(sdk)) {
                    packageCommandLine.add(sdk);
                } else {
                    packageCommandLine.add("iphoneos");
                }
                packageCommandLine.addAll(Lists.newArrayList("PackageApplication", "-v", app.getRemote(), "-o", ipaLocation.getRemote()));
                if (!StringUtils.isEmpty(embeddedProfileFile)) {
                    packageCommandLine.add("--embed");
                    packageCommandLine.add(embeddedProfileFile);
                }

                returnCode = launcher.launch().envs(envs).stdout(listener).pwd(projectRoot).cmds(packageCommandLine).join();
                if (returnCode > 0) {
                    listener.getLogger().println("Failed to build " + ipaLocation.getName());
                    continue;
                }

                // also zip up the symbols, if present
                returnCode = launcher.launch().envs(envs).stdout(listener).pwd(buildDirectory).cmds("zip", "-r", "-T", "-y", baseName + "-dSYM.zip", app.getBaseName() + ".app.dSYM").join();
                if (returnCode > 0) {
                    listener.getLogger().println(Messages.XCodeBuilder_zipFailed(baseName));
                    continue;
                }

                payload.deleteRecursive();
            }
        }

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private String xcodebuildPath = "/usr/bin/xcodebuild";
        private String agvtoolPath = "/usr/bin/agvtool";
        private String xcrunPath = "/usr/bin/xcrun";

        public FormValidation doCheckXcodebuildPath(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(Messages.XCodeBuilder_xcodebuildPathNotSet());
            } else {
                // TODO: check that the file exists (and if an agent is used ?)
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAgvtoolPath(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.isEmpty(value))
                return FormValidation.error(Messages.XCodeBuilder_agvtoolPathNotSet());
            else {
                // TODO: check that the file exists (and if an agent is used ?)
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckXcrunPath(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.isEmpty(value))
                return FormValidation.error(Messages.XCodeBuilder_xcrunPathNotSet());
            else {
                // TODO: check that the file exists (and if an agent is used ?)
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName() {
            return Messages.XCodeBuilder_xcode();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        public String getAgvtoolPath() {
            return agvtoolPath;
        }

        public String getXcodebuildPath() {
            return xcodebuildPath;
        }

        public String getXcrunPath() {
            return xcrunPath;
        }

        public void setXcodebuildPath(String xcodebuildPath) {
          this.xcodebuildPath = xcodebuildPath;
        }

        public void setAgvtoolPath(String agvtoolPath) {
          this.agvtoolPath = agvtoolPath;
        }

        public void setXcrunPath(String xcrunPath) {
          this.xcrunPath = xcrunPath;
        }
      }
}

