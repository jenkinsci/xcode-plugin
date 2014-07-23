package au.com.rayh;

import org.kohsuke.stapler.DataBoundConstructor;

public class Keychain {
    private String keychainName;
    private String keychainPath;
    private String keychainPassword;
    private String keychainTimeout;
    private Boolean inSearchPath;

    public Keychain() {
    }

    @DataBoundConstructor
    public Keychain(String keychainName, String keychainPath, String keychainPassword, String keychainTimeout, Boolean inSearchPath) {
        this.keychainName = keychainName;
        this.keychainPath = keychainPath;
        this.keychainPassword = keychainPassword;
        this.keychainTimeout = keychainTimeout;
        this.inSearchPath = inSearchPath;
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

    public String getKeychainTimeout() {
        return keychainTimeout;
    }

    public void setKeychainTimeout(String keychainTimeout) {
        this.keychainTimeout = keychainTimeout;
    }

	public Boolean isInSearchPath() {
		return inSearchPath;
	}

	public void setInSearchPath(Boolean inSearchPath) {
		this.inSearchPath = inSearchPath;
	}

}
