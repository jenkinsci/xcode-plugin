package au.com.rayh;

import org.kohsuke.stapler.DataBoundConstructor;

public class Keychain {
    private String keychainName;
    private String keychainPath;
    private String keychainPassword;

    public Keychain() {
    }

    @DataBoundConstructor
    public Keychain(String keychainName, String keychainPath, String keychainPassword) {
        this.keychainName = keychainName;
        this.keychainPath = keychainPath;
        this.keychainPassword = keychainPassword;
    }

    public String getKeychainName() {
        return keychainName;
    }

    public void setKeychainName(String keychainName) {
        this.keychainName = keychainName;
    }

    public String getKeychainPath() {
        return keychainPath;
    }

    public void setKeychainPath(String keychainPath) {
        this.keychainPath = keychainPath;
    }

    public String getKeychainPassword() {
        return keychainPassword;
    }

    public void setKeychainPassword(String keychainPassword) {
        this.keychainPassword = keychainPassword;
    }
}
