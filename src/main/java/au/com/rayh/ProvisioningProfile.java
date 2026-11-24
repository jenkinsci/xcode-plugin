package au.com.rayh;

import hudson.EnvVars;
import org.kohsuke.stapler.DataBoundConstructor;

public class ProvisioningProfile {

    private static final String PLIST_FORMAT = "<key>${APP_ID}</key><string>${UUID}</string>";

    private String provisioningProfileAppId;
    private String provisioningProfileUUID;

    public ProvisioningProfile() {}

    @DataBoundConstructor
    public ProvisioningProfile(String provisioningProfileAppId, String provisioningProfileUUID) {
        this.provisioningProfileAppId = provisioningProfileAppId;
        this.provisioningProfileUUID = provisioningProfileUUID;
    }

    public String getProvisioningProfileAppId() {
        return provisioningProfileAppId;
    }

    public void setProvisioningProfileAppId(String provisioningProfileAppId) {
        this.provisioningProfileAppId = provisioningProfileAppId;
    }

    public String getProvisioningProfileUUID() {
        return provisioningProfileUUID;
    }

    public void setProvisioningProfileUUID(String provisioningProfileUUID) {
        this.provisioningProfileUUID = provisioningProfileUUID;
    }

    public String toPlist(EnvVars envs) {
        return PLIST_FORMAT
                .replace("${APP_ID}", envs.expand(provisioningProfileAppId))
                .replace("${UUID}", envs.expand(provisioningProfileUUID));
    }
}
