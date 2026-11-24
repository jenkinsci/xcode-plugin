package au.com.rayh;

import hudson.FilePath;

// import org.kohsuke.stapler.DataBoundConstructor;

public class InfoPlist {

    private FilePath infoPlistFile;
    private String cfBundleIdentifier;
    private String cfBundleVersion;
    private String cfBundleShortVersionString;

    InfoPlist() {
        infoPlistFile = null;
        cfBundleIdentifier = null;
        cfBundleVersion = null;
        cfBundleShortVersionString = null;
    }

    // @DataBoundConstructor
    InfoPlist(
            FilePath infoPlistFile,
            String cfBundleIdentifier,
            String cfBundleVersion,
            String cfBundleShortVersionString) {
        this.infoPlistFile = infoPlistFile;
        this.cfBundleIdentifier = cfBundleIdentifier;
        this.cfBundleVersion = cfBundleVersion;
        this.cfBundleShortVersionString = cfBundleShortVersionString;
    }

    public FilePath getInfoPlistFile() {
        return this.infoPlistFile;
    }

    public void setInfoPlistFile(FilePath infoPlistFile) {
        this.infoPlistFile = infoPlistFile;
    }

    public String getCfBundleIdentifier() {
        return this.cfBundleIdentifier;
    }

    public void setCfBundleIdentifier(String cfBundleIdentifier) {
        this.cfBundleIdentifier = cfBundleIdentifier;
    }

    public String getCfBundleVersion() {
        return this.cfBundleVersion;
    }

    public void setCfBundleVersion(String cfBundleVersion) {
        this.cfBundleVersion = cfBundleVersion;
    }

    public String getCfBundleShortVersionString() {
        return this.cfBundleShortVersionString;
    }

    public void setCfBundleShortVersionString(String cfBundleShortVersionString) {
        this.cfBundleShortVersionString = cfBundleShortVersionString;
    }
}
