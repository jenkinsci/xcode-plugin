package au.com.rayh;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class Keychain {
    private String keychainName;
    private String keychainPath;
    private Secret keychainPassword;
    private Boolean inSearchPath;

    public Keychain() {
    }

    @Deprecated
    @DataBoundConstructor
    public Keychain(String keychainName, String keychainPath, String keychainPassword, Boolean inSearchPath) {
        this.keychainName = keychainName;
        this.keychainPath = keychainPath;
        this.keychainPassword = Secret.fromString(keychainPassword);
        this.inSearchPath = inSearchPath;
    }

    public String getKeychainName() {
        return keychainName;
    }

    @DataBoundSetter
    public void setKeychainName(String keychainName) {
        this.keychainName = keychainName;
    }

    public String getKeychainPath() {
        return keychainPath;
    }

    @DataBoundSetter
    public void setKeychainPath(String keychainPath) {
        this.keychainPath = keychainPath;
    }

    public Secret getKeychainPassword() {
        return keychainPassword;
    }

    @DataBoundSetter
    public void setKeychainPassword(Secret keychainPassword) {
        this.keychainPassword = keychainPassword;
    }

    public Boolean isInSearchPath() {
        return inSearchPath;
    }

    @DataBoundSetter
    public void setInSearchPath(Boolean inSearchPath) {
        this.inSearchPath = inSearchPath;
    }
}
