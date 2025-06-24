/*
 * The MIT License
 *
 * Copyright (c) 2013 Maur�cio Hanika
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

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import jakarta.servlet.ServletException;

/**
 * Stores global configuration for XCode.
 *
 * @since 1.4
 */
@Extension
public final class GlobalConfigurationImpl extends GlobalConfiguration {
    private static final Logger LOGGER = Logger.getLogger(GlobalConfigurationImpl.class.getName());
    @CheckForNull
    private String xcodebuildPath = "/usr/bin/xcodebuild";
    @CheckForNull
    private String xcrunPath = "/usr/bin/xcrun";
    @CheckForNull
    private String agvtoolPath = "/usr/bin/agvtool";
    // No one uses defaultKeychain.
    @Deprecated
    @CheckForNull
    private String defaultKeychain = "";
    @Deprecated
    @CheckForNull
    private ArrayList<Keychain> keychains = new ArrayList<>();
    @Deprecated
    @CheckForNull
    private ArrayList<Team> teams = new ArrayList<>();

    @DataBoundSetter
    public void setXcodebuildPath(String xcodebuildPath) {
        this.xcodebuildPath = xcodebuildPath;
    }

    @CheckForNull
    public String getXcodebuildPath() {
        return xcodebuildPath;
    }

    @DataBoundSetter
    public void setXcrunPath(String xcrunPath) {
        this.xcrunPath = xcrunPath;
    }

    @CheckForNull
    public String getXcrunPath() {
        return xcrunPath;
    }

    @DataBoundSetter
    public void setAgvtoolPath(String agvtoolPath) {
        this.agvtoolPath = agvtoolPath;
    }

    @CheckForNull
    public String getAgvtoolPath() {
        return agvtoolPath;
    }

    @Deprecated
    @DataBoundSetter
    public void setDefaultKeychain(String defaultKeychain) {
        this.defaultKeychain = defaultKeychain;
    }

    @Deprecated
    @CheckForNull
    public String getDefaultKeychain() {
        return defaultKeychain;
    }

    @Deprecated
    @DataBoundSetter
    public void setKeychains(ArrayList<Keychain> keychains) {
        this.keychains = keychains;
    }

    @Deprecated
    public ArrayList<Keychain> getKeychains() {
	return keychains == null ? keychains : new ArrayList<Keychain>();
    }

    @Deprecated
    @DataBoundSetter
    public void setTeams(ArrayList<Team> teams) {
        this.teams = teams;
    }

    @Deprecated
    public ArrayList<Team> getTeams() {
        return teams;
    }

    @DataBoundConstructor
    public GlobalConfigurationImpl() {
        super();
        load();
	LOGGER.fine("[Xcode] Default constructor: " + getKeychains().size());
    }

    @Deprecated
    public GlobalConfigurationImpl(String xcodebuildPath, String xcrunPath, String agvtoolPath, String defaultKeychain, ArrayList<Keychain> keychains, ArrayList<Team> teams) {
        this();

        this.setXcodebuildPath(xcodebuildPath);
        this.setXcrunPath(xcrunPath);
        this.setAgvtoolPath(agvtoolPath);
        this.setDefaultKeychain(defaultKeychain);
        this.setKeychains(keychains);
        this.setTeams(teams);

        LOGGER.fine("[Xcode] DataBoundConstructor: keychains.size " + keychains.size());
        if(teams != null) {
            LOGGER.fine("[Xcode] DataBoundConstructor: teams.size " + teams.size());
        }
    }

    @Deprecated
    public GlobalConfigurationImpl(String xcodebuildPath, String xcrunPath, String agvtoolPath, String defaultKeychain, ArrayList<Keychain> keychains) {
        this(xcodebuildPath, xcrunPath, agvtoolPath, defaultKeychain, keychains, null);
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
        if (StringUtils.isEmpty(value)) {
            return FormValidation.error(Messages.XCodeBuilder_agvtoolPathNotSet());
        } else {
            // TODO: check that the file exists (and if an agent is used ?)
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckXcrunPath(@QueryParameter String value) throws IOException, ServletException {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.error(Messages.XCodeBuilder_xcrunPathNotSet());
        } else {
            // TODO: check that the file exists (and if an agent is used ?)
        }
        return FormValidation.ok();
    }

    /* Useless method that doesn't make sense.
    public FormValidation doCheckDefaultKeychain(@QueryParameter String value) throws IOException, ServletException {
        if (!StringUtils.isEmpty(value)) {
            Boolean foundKeychain = false;
            for (Keychain k : getKeychains()) {
                if (k.getKeychainName().equals(value)) {
                    foundKeychain = true;
                    break;
                }
            }

            if (!foundKeychain) {
                return FormValidation.error(Messages.OSXKeychainBuildWrapper_invalidDefaultKeychainName(value));
            }
        }

        return FormValidation.ok();
    }
     */

    public AutoCompletionCandidates doAutoCompleteDefaultKeychain(@QueryParameter String value) {
        AutoCompletionCandidates c = new AutoCompletionCandidates();
        for (Keychain keychain : getKeychains()) {
            if (keychain.getKeychainName().toLowerCase().startsWith(value.toLowerCase())) {
                c.add(keychain.getKeychainName());
            }
        }
        return c;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        // indicates that this builder can be used with all kinds of project types
        return true;
    }

    @Override
    public String getDisplayName() {
        return Messages.XCodeBuilder_xcode();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject formData) throws FormException {
        req.bindJSON(this, formData);
        setKeychains(new ArrayList<>(req.bindParametersToList(Keychain.class, "keychain.")));
        setTeams(new ArrayList<>(req.bindParametersToList(Team.class, "team.")));
        save();

        return super.configure(req, formData);
    }
}
