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
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.dd.plist.*;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;

/**
 * @author Ray Hilton
 */
public class XCodeBuilder extends Builder {
    /**
     * @since 1.0
     */
    public final Boolean cleanBeforeBuild;
    /**
     * @since 1.3
     */
    public final Boolean cleanTestReports;
    /**
     * @since 1.0
     */
    public final String configuration;
    /**
     * @since 1.0
     */
    public final String target;
    /**
     * @since 1.0
     */
    public final String sdk;
    /**
     * @since 1.1
     */
    public final String symRoot;
    /**
     * @since 1.2
     */
    public final String configurationBuildDir;
    /**
     * @since 1.0
     */
    public final String xcodeProjectPath;
    /**
     * @since 1.0
     */
    public final String xcodeProjectFile;
    /**
     * @since 1.3
     */
    public final String xcodebuildArguments;
    /**
     * @since 1.2
     */
    public final String xcodeSchema;
    /**
     * @since 1.2
     */
    public final String xcodeWorkspaceFile;
    /**
     * @since 1.0
     */
    public final String embeddedProfileFile;
    /**
     * @since 1.0
     */
    public final String cfBundleVersionValue;
    /**
     * @since 1.0
     */
    public final String cfBundleShortVersionStringValue;
    /**
     * @since 1.0
     */
    public final Boolean buildIpa;
    /**
     * @since 1.0
     */
    public final Boolean unlockKeychain;
    /**
     * @since 1.4
     */
    public final String keychainName;
    /**
     * @since 1.0
     */
    public final String keychainPath;
    /**
     * @since 1.0
     */
    public final String keychainPwd;
    /**
     * @since 1.3.3
     */
    public final String codeSigningIdentity;
    /**
     * @since 1.4
     */
    public final Boolean allowFailingBuildResults;
    /**
     * @since 1.4.0
     */
    public final String ipaName;
    
    protected String defaultKeychain;
    protected String[] keychains;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public XCodeBuilder(Boolean buildIpa, Boolean cleanBeforeBuild, Boolean cleanTestReports, String configuration, String target, String sdk, String xcodeProjectPath, String xcodeProjectFile, String xcodebuildArguments, String embeddedProfileFile, String cfBundleVersionValue, String cfBundleShortVersionStringValue, Boolean unlockKeychain, String keychainName, String keychainPath, String keychainPwd, String symRoot, String xcodeWorkspaceFile, String xcodeSchema, String configurationBuildDir, String codeSigningIdentity, String ipaName, Boolean allowFailingBuildResults) {
        this.buildIpa = buildIpa;
        this.sdk = sdk;
        this.target = target;
        this.cleanBeforeBuild = cleanBeforeBuild;
        this.cleanTestReports = cleanTestReports;
        this.configuration = configuration;
        this.xcodeProjectPath = xcodeProjectPath;
        this.xcodeProjectFile = xcodeProjectFile;
        this.xcodebuildArguments = xcodebuildArguments;
        this.keychainName = keychainName;
        this.xcodeWorkspaceFile = xcodeWorkspaceFile;
        this.xcodeSchema = xcodeSchema;
        this.embeddedProfileFile = embeddedProfileFile;
        this.codeSigningIdentity = codeSigningIdentity;
        this.cfBundleVersionValue = cfBundleVersionValue;
        this.cfBundleShortVersionStringValue = cfBundleShortVersionStringValue;
        this.unlockKeychain = unlockKeychain;
        this.keychainPath = keychainPath;
        this.keychainPwd = keychainPwd;
        this.symRoot = symRoot;
        this.configurationBuildDir = configurationBuildDir;
        this.allowFailingBuildResults = allowFailingBuildResults;
        this.ipaName = ipaName;
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

        // Start expanding all string variables in parameters
        // NOTE: we currently use variable shadowing to avoid having to rewrite all code (and break pull requests), this will be cleaned up at later stage.
        String configuration = envs.expand(this.configuration);
        String target = envs.expand(this.target);
        String sdk = envs.expand(this.sdk);
        String symRoot = envs.expand(this.symRoot);
        String configurationBuildDir = envs.expand(this.configurationBuildDir);
        String xcodeProjectPath = envs.expand(this.xcodeProjectPath);
        String xcodeProjectFile = envs.expand(this.xcodeProjectFile);
        String xcodebuildArguments = envs.expand(this.xcodebuildArguments);
        String xcodeSchema = envs.expand(this.xcodeSchema);
        String xcodeWorkspaceFile = envs.expand(this.xcodeWorkspaceFile);
        String embeddedProfileFile = envs.expand(this.embeddedProfileFile);
        String cfBundleVersionValue = envs.expand(this.cfBundleVersionValue);
        String cfBundleShortVersionStringValue = envs.expand(this.cfBundleShortVersionStringValue);
        String codeSigningIdentity = envs.expand(this.codeSigningIdentity);
        String ipaName = envs.expand(this.ipaName);
        // End expanding all string variables in parameters  

        // Set the working directory
        if (!StringUtils.isEmpty(xcodeProjectPath)) {
            projectRoot = projectRoot.child(xcodeProjectPath);
        }
        listener.getLogger().println(Messages.XCodeBuilder_workingDir(projectRoot));

        // Infer as best we can the build platform
        String buildPlatform = "iphoneos";
        if (!StringUtils.isEmpty(sdk)) {
            if (StringUtils.contains(sdk.toLowerCase(), "iphonesimulator")) {
                // Building for the simulator
                buildPlatform = "iphonesimulator";
            }
        }

        // Set the build directory and the symRoot
        //
        String symRootValue = null;
        if (!StringUtils.isEmpty(symRoot)) {
            try {
                // If not empty we use the Token Expansion to replace it
                // https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin
                symRootValue = TokenMacro.expandAll(build, listener, symRoot).trim();
            } catch (MacroEvaluationException e) {
                listener.error(Messages.XCodeBuilder_symRootMacroError(e.getMessage()));
                return false;
            }
        }

        String configurationBuildDirValue = null;
        FilePath buildDirectory;
        if (!StringUtils.isEmpty(configurationBuildDir)) {
            try {
                configurationBuildDirValue = TokenMacro.expandAll(build, listener, configurationBuildDir).trim();
            } catch (MacroEvaluationException e) {
                listener.error(Messages.XCodeBuilder_configurationBuildDirMacroError(e.getMessage()));
                return false;
            }
        }

        if (configurationBuildDirValue != null) {
            // If there is a CONFIGURATION_BUILD_DIR, that overrides any use of SYMROOT. Does not require the build platform and the configuration.
            buildDirectory = new FilePath(projectRoot.getChannel(), configurationBuildDirValue);
        } else if (symRootValue != null) {
            // If there is a SYMROOT specified, compute the build directory from that.
            buildDirectory = new FilePath(projectRoot.getChannel(), symRootValue).child(configuration + "-" + buildPlatform);
        } else {
            // Assume its a build for the handset, not the simulator.
            buildDirectory = projectRoot.child("build").child(configuration + "-" + buildPlatform);
        }

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
        if (StringUtils.isEmpty(cfBundleShortVersionString))
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringNotFound());
        else
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringFound(cfBundleShortVersionString));
        listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringValue(cfBundleShortVersionString));

        output.reset();

        // Try to read CFBundleVersion from project
        listener.getLogger().println(Messages.XCodeBuilder_fetchingCFBundleVersion());
        String cfBundleVersion = "";
        returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getAgvtoolPath(), "vers", "-terse").stdout(output).pwd(projectRoot).join();
        // only use this version number if we found it
        if (returnCode == 0)
            cfBundleVersion = output.toString().trim();
        if (StringUtils.isEmpty(cfBundleVersion))
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionNotFound());
        else
            listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionFound(cfBundleShortVersionString));
        listener.getLogger().println(Messages.XCodeBuilder_CFBundleVersionValue(cfBundleVersion));
        
        String buildDescription = cfBundleShortVersionString + " (" + cfBundleVersion + ")";
        XCodeAction a = new XCodeAction(buildDescription);
        build.addAction(a);

        // Update the Marketing version (CFBundleShortVersionString)
        if (!StringUtils.isEmpty(cfBundleShortVersionStringValue)) {
            try {
                // If not empty we use the Token Expansion to replace it
                // https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin
                cfBundleShortVersionString = TokenMacro.expandAll(build, listener, cfBundleShortVersionStringValue);
                listener.getLogger().println(Messages.XCodeBuilder_CFBundleShortVersionStringUpdate(cfBundleShortVersionString));
                returnCode = launcher.launch().envs(envs).cmds(getDescriptor().getAgvtoolPath(), "new-marketing-version", cfBundleShortVersionString).stdout(listener).pwd(projectRoot).join();
                if (returnCode > 0) {
                    listener.fatalError(Messages.XCodeBuilder_CFBundleShortVersionStringUpdateError(cfBundleShortVersionString));
                    return false;
                }
            } catch (MacroEvaluationException e) {
                listener.fatalError(Messages.XCodeBuilder_CFBundleShortVersionStringMacroError(e.getMessage()));
                // Fails the build
                return false;
            }
        }

        // Update the Technical version (CFBundleVersion)
        if (!StringUtils.isEmpty(cfBundleVersionValue)) {
            try {
                // If not empty we use the Token Expansion to replace it
                // https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin
                cfBundleVersion = TokenMacro.expandAll(build, listener, cfBundleVersionValue);
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
            listener.getLogger().println(Messages.XCodeBuilder_cleaningBuildDir(buildDirectory.absolutize().getRemote()));
            buildDirectory.deleteRecursive();
        }

        // remove test-reports and *.ipa
        if (cleanTestReports != null && cleanTestReports) {
            listener.getLogger().println(Messages.XCodeBuilder_cleaningTestReportsDir(projectRoot.child("test-reports").absolutize().getRemote()));
            projectRoot.child("test-reports").deleteRecursive();
		}

        if (unlockKeychain == null && unlockKeychain) {
            // Get the current keychains and default keychain
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            launcher.launch().envs(envs).cmds("/usr/bin/security", "list-keychains").stdout(outputStream).pwd(projectRoot).join();
            
            String _keychains = new String(outputStream.toByteArray(), Charset.defaultCharset());
            keychains = _keychains.split("\\r?\\n");
            for (int i = 0; i < keychains.length; i++) {
                String line = keychains[i];
                keychains[i] = line.trim();
            }
            
            outputStream.reset();
            launcher.launch().envs(envs).cmds("/usr/bin/security", "default-keychain").stdout(outputStream).pwd(projectRoot).join();
            
            defaultKeychain = (new String(outputStream.toByteArray(), Charset.defaultCharset())).trim();
            
            // System.out.println("Def keychain: " + defaultKeychain);
            // System.out.println("All keychains: " + StringUtils.join(keychains, ", "));
            
            // Let's unlock the keychain
            Keychain keychain = getKeychain();
            if(keychain == null)
            {
                listener.fatalError(Messages.XCodeBuilder_keychainNotConfigured());
                return false;
            }
            String keychainPath = envs.expand(keychain.getKeychainPath());
            String keychainPwd = envs.expand(keychain.getKeychainPassword());
            launcher.launch().envs(envs).cmds("/usr/bin/security", "list-keychains", "-s", keychainPath).stdout(listener).pwd(projectRoot).join();
            launcher.launch().envs(envs).cmds("/usr/bin/security", "default-keychain", "-d", "user", "-s", keychainPath).stdout(listener).pwd(projectRoot).join();
            launcher.launch().envs(envs).cmds("/usr/bin/security", "show-keychain-info", keychainPath).stdout(listener).pwd(projectRoot).join();
            if (StringUtils.isEmpty(keychainPwd))
                returnCode = launcher.launch().envs(envs).cmds("/usr/bin/security", "unlock-keychain", keychainPath).stdout(listener).pwd(projectRoot).join();
            else
                returnCode = launcher.launch().envs(envs).cmds("/usr/bin/security", "unlock-keychain", "-p", keychainPwd, keychainPath).masks(false, false, false, true, false).stdout(listener).pwd(projectRoot).join();
            if (returnCode > 0) {
                this.restoreKeychains(launcher, listener, projectRoot, envs);
                
                listener.fatalError(Messages.XCodeBuilder_unlockKeychainFailed());
                return false;
            }
        }

        // display useful setup information
        listener.getLogger().println(Messages.XCodeBuilder_DebugInfoLineDelimiter());
        listener.getLogger().println(Messages.XCodeBuilder_DebugInfoAvailablePProfiles());
        /*returnCode =*/ launcher.launch().envs(envs).cmds("/usr/bin/security", "find-identity", "-p", "codesigning", "-v").stdout(listener).pwd(projectRoot).join();

        if (!StringUtils.isEmpty(codeSigningIdentity)) {
            listener.getLogger().println(Messages.XCodeBuilder_DebugInfoCanFindPProfile());
            /*returnCode =*/ launcher.launch().envs(envs).cmds("/usr/bin/security", "find-certificate", "-a", "-c", codeSigningIdentity, "-Z", "|", "grep", "^SHA-1").stdout(listener).pwd(projectRoot).join();
            // We could fail here, but this doesn't seem to work as it should right now (output not properly redirected. We might need a parser)
        }

        listener.getLogger().println(Messages.XCodeBuilder_DebugInfoAvailableSDKs());
        /*returnCode =*/ launcher.launch().envs(envs).cmds(getDescriptor().getXcodebuildPath(), "-showsdks").stdout(listener).pwd(projectRoot).join();
        {
            List<String> commandLine = Lists.newArrayList(getDescriptor().getXcodebuildPath());
            commandLine.add("-list");
            // xcodebuild -list -workspace $workspace
            listener.getLogger().println(Messages.XCodeBuilder_DebugInfoAvailableSchemes());
            if (!StringUtils.isEmpty(xcodeWorkspaceFile)) {
                commandLine.add("-workspace");
                commandLine.add(xcodeWorkspaceFile + ".xcworkspace");
            } else if (!StringUtils.isEmpty(xcodeProjectFile)) {
                commandLine.add("-project");
                commandLine.add(xcodeProjectFile);
            }
            returnCode = launcher.launch().envs(envs).cmds(commandLine).stdout(listener).pwd(projectRoot).join();
            if (returnCode > 0) {
                this.restoreKeychains(launcher, listener, projectRoot, envs);
                return false;
            }
        }
        listener.getLogger().println(Messages.XCodeBuilder_DebugInfoLineDelimiter());

        // Build
        StringBuilder xcodeReport = new StringBuilder(Messages.XCodeBuilder_invokeXcodebuild());
        XCodeBuildOutputParser reportGenerator = new XCodeBuildOutputParser(projectRoot, listener);
        List<String> commandLine = Lists.newArrayList(getDescriptor().getXcodebuildPath());

        // Prioritizing schema over target setting
        if (!StringUtils.isEmpty(xcodeSchema)) {
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

		if (!StringUtils.isEmpty(configuration)) {
			commandLine.add("-configuration");
			commandLine.add(configuration);
			xcodeReport.append(", configuration: ").append(configuration);
		}

        // Add the build number
        commandLine.add("BUILD_NUMBER=" + envs.get("BUILD_NUMBER", "0"));
        
        if (cleanBeforeBuild) {
            commandLine.add("clean");
            xcodeReport.append(", clean: YES");
        } else {
            xcodeReport.append(", clean: NO");
        }
        
        commandLine.add("build");

        if (!StringUtils.isEmpty(symRootValue)) {
            commandLine.add("SYMROOT=" + symRootValue);
            xcodeReport.append(", symRoot: ").append(symRootValue);
        } else {
            xcodeReport.append(", symRoot: DEFAULT");
        }

        // CONFIGURATION_BUILD_DIR
        if (!StringUtils.isEmpty(configurationBuildDirValue)) {
            commandLine.add("CONFIGURATION_BUILD_DIR=" + configurationBuildDirValue);
            xcodeReport.append(", configurationBuildDir: ").append(configurationBuildDirValue);
        } else {
            xcodeReport.append(", configurationBuildDir: DEFAULT");
        }

        // handle code signing identities
        if (!StringUtils.isEmpty(codeSigningIdentity)) {
            commandLine.add("CODE_SIGN_IDENTITY=" + codeSigningIdentity);
            xcodeReport.append(", codeSignIdentity: ").append(codeSigningIdentity);
        } else {
            xcodeReport.append(", codeSignIdentity: DEFAULT");
        }

        // Additional (custom) xcodebuild arguments
        if (!StringUtils.isEmpty(xcodebuildArguments)) {
            commandLine.addAll(splitXcodeBuildArguments(xcodebuildArguments));
        }

        listener.getLogger().println(xcodeReport.toString());
        returnCode = launcher.launch().envs(envs).cmds(commandLine).stdout(reportGenerator.getOutputStream()).pwd(projectRoot).join();
        if (allowFailingBuildResults != null && allowFailingBuildResults.booleanValue() == false) {
			// Unlock the default keychains after building
	        this.restoreKeychains(launcher, listener, projectRoot, envs);

            if (reportGenerator.getExitCode() != 0) return false;
            if (returnCode > 0) return false;
        }

        // Package IPA
        if (buildIpa) {

            if (!buildDirectory.exists() || !buildDirectory.isDirectory()) {
                listener.fatalError(Messages.XCodeBuilder_NotExistingBuildDirectory(buildDirectory.absolutize().getRemote()));
                return false;                
            }
            // clean IPA
            listener.getLogger().println(Messages.XCodeBuilder_cleaningIPA());
            for (FilePath path : buildDirectory.list("*.ipa")) {
                path.delete();
            }
            listener.getLogger().println(Messages.XCodeBuilder_cleaningDSYM());
            for (FilePath path : buildDirectory.list("*-dSYM.zip")) {
                path.delete();
            }
            // packaging IPA
            listener.getLogger().println(Messages.XCodeBuilder_packagingIPA());
            List<FilePath> apps = buildDirectory.list(new AppFileFilter());
            // FilePath is based on File.listFiles() which can randomly fail | http://stackoverflow.com/questions/3228147/retrieving-the-underlying-error-when-file-listfiles-return-null
            if (apps == null) {
                listener.fatalError(Messages.XCodeBuilder_NoAppsInBuildDirectory(buildDirectory.absolutize().getRemote()));
                return false;                
            }

            for (FilePath app : apps) {
                String version = "";
                try {
                    File file = new File(app.absolutize().child("Info.plist").getRemote());

                    NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(file);
                    version = rootDict.objectForKey("CFBundleVersion").toString();

                    if (StringUtils.isEmpty(version)) {
                        version = rootDict.objectForKey("CFBundleShortVersionString").toString();
                    }

                    if (StringUtils.isEmpty(version)) {
                        version = Integer.toString(build.getNumber());
                    }
                } 
                catch(Exception ex) {
                    listener.getLogger().println("Failed to get version: " + ex.toString());
                }

                File file = new File(app.absolutize().getRemote());
                String lastModified = new SimpleDateFormat("yyyy.MM.dd").format(new Date(file.lastModified()));

                String baseName = app.getBaseName().replaceAll(" ", "_") + (StringUtils.isEmpty(version) ? "" : "_" + version) + "_" + lastModified;
                // Custom name stuff
                if (! StringUtils.isEmpty(ipaName)) {
                    Map valuesMap = new HashMap();
                    valuesMap.put("VERSION", version);
                    valuesMap.put("BUILD_DATE", lastModified);

                    StrSubstitutor sub = new StrSubstitutor(valuesMap);
                    baseName = sub.replace(ipaName);
                }

                FilePath ipaLocation = buildDirectory.child(baseName + ".ipa");

                FilePath payload = buildDirectory.child("Payload");
                payload.deleteRecursive();
                payload.mkdirs();

                listener.getLogger().println("Packaging " + app.getBaseName() + ".app => " + ipaLocation.absolutize().getRemote());
                List<String> packageCommandLine = new ArrayList<String>();
                packageCommandLine.add(getDescriptor().getXcrunPath());
                packageCommandLine.add("-sdk");

                if (!StringUtils.isEmpty(sdk)) {
                    packageCommandLine.add(sdk);
                } else {
                    packageCommandLine.add(buildPlatform);
                }
                packageCommandLine.addAll(Lists.newArrayList("PackageApplication", "-v", app.absolutize().getRemote(), "-o", ipaLocation.absolutize().getRemote()));
                if (!StringUtils.isEmpty(embeddedProfileFile)) {
                    packageCommandLine.add("--embed");
                    packageCommandLine.add(embeddedProfileFile);
                }
                if (!StringUtils.isEmpty(codeSigningIdentity)) {                   
                    packageCommandLine.add("--sign");
                    packageCommandLine.add(codeSigningIdentity);
                }

                returnCode = launcher.launch().envs(envs).stdout(listener).pwd(projectRoot).cmds(packageCommandLine).join();
                if (returnCode > 0) {
                    listener.getLogger().println("Failed to build " + ipaLocation.absolutize().getRemote());
                    continue;
                }

                // also zip up the symbols, if present
                returnCode = launcher.launch().envs(envs).stdout(listener).pwd(buildDirectory).cmds("ditto", "-c", "-k", "--keepParent", "-rsrc", app.absolutize().getRemote() + ".dSYM", baseName + "-dSYM.zip").join();
                if (returnCode > 0) {
                    listener.getLogger().println(Messages.XCodeBuilder_zipFailed(baseName));
                    continue;
                }

                payload.deleteRecursive();
            }
        }

        return true;
    }
    
    enum ParserState {
        NORMAL,
        IN_QUOTE,
        IN_DOUBLE_QUOTE,
        ESCAPE_CHAR
    };
    
    protected void restoreKeychains(Launcher launcher, BuildListener listener, FilePath projectRoot, EnvVars envs) throws java.io.IOException, java.lang.InterruptedException {
        ArrayList<String> parameters = new ArrayList<String>() {{
            add("/usr/bin/security");
            add("list-keychains");
            add("-s");
        }};

    public Keychain getKeychain() {
        if(!StringUtils.isEmpty(keychainPath)) {
            return new Keychain("", keychainPath, keychainPwd);
        }

        for (Keychain keychain : getDescriptor().getKeychains()) {
            if(keychain.getKeychainName().equals(keychainName))
                return keychain;
        }

        return null;
    }

        for (String s : keychains) {
            parameters.add(s.substring(1, s.length() - 1));
        }

        launcher.launch().envs(envs).cmds(parameters).stdout(listener).pwd(projectRoot).join();
        launcher.launch().envs(envs).cmds("/usr/bin/security", "default-keychain", "-d", "user", "-s", defaultKeychain.substring(1, defaultKeychain.length() - 1)).stdout(listener).pwd(projectRoot).join();
    }

    static List<String> splitXcodeBuildArguments(String xcodebuildArguments) {
        if (xcodebuildArguments == null || xcodebuildArguments.length() == 0) {
            return new ArrayList<String>(0);
        }

        ParserState state = ParserState.NORMAL;
        final StringTokenizer tok = new StringTokenizer(xcodebuildArguments, "\"'\\ ", true);
        final List<String> result = new ArrayList<String>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;
        String nextTok;
        while (tok.hasMoreTokens()) {
            nextTok = tok.nextToken();
            switch (state) {
            case IN_QUOTE:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = ParserState.NORMAL;
                } else {
                    current.append(nextTok);
                }
                break;
            case IN_DOUBLE_QUOTE:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = ParserState.NORMAL;
                } else {
                    current.append(nextTok);
                }
                break;
            case ESCAPE_CHAR:
                current.append(nextTok);
                state = ParserState.NORMAL;
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = ParserState.IN_QUOTE;
                } else if ("\"".equals(nextTok)) {
                    state = ParserState.IN_DOUBLE_QUOTE;
                } else if ("\\".equals(nextTok)) {
                    state = ParserState.ESCAPE_CHAR;
                } else if (" ".equals(nextTok) && !(current.length() > 0 && current.charAt(current.length() - 1) == '\\')) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() != 0) {
            result.add(current.toString());
        }
        if (state == ParserState.IN_QUOTE || state == ParserState.IN_DOUBLE_QUOTE) {
            throw new IllegalArgumentException(String.format("Inconsistent quotes: %s", xcodebuildArguments));
        }
        return result;
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
        private final CopyOnWriteList<Keychain> keychains = new CopyOnWriteList<Keychain>();

        public DescriptorImpl() {
            load();
        }

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
            keychains.replaceBy(req.bindParametersToList(Keychain.class, "keychain."));
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

        public Iterable<Keychain> getKeychains() {
            return keychains;
        }
    }
}

