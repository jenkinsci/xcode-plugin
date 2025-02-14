package au.com.rayh;

import com.dd.plist.NSDictionary; 

public class BuildConfiguration {
  public String uuid;
  public String name;
  public String bundleIdentifier;
  public String infoPlistFile;
  //public String codeSignStyle;
  public String developmentTeamId;
  public String provisioningProfileUUID;
  public String provisioningProfileSpecifier;
  public String productName;

  BuildConfiguration(NSDictionary objectsDict, String buildConfigurationUUID, Boolean automatic) {
    NSDictionary buildConfigurationDict = ((NSDictionary)objectsDict.objectForKey(buildConfigurationUUID.toString()));
    //String baseConfigurationReference = buildConfigurationDict.objectForKey("baseConfigurationReference").toString();
    this.uuid = buildConfigurationUUID.toString();
    this.name = buildConfigurationDict.objectForKey("name").toString();
    System.out.println("buildConfigurationUUID: " + this.uuid);
    NSDictionary buildSettingsDict = ((NSDictionary)buildConfigurationDict.objectForKey("buildSettings"));
    if ( buildSettingsDict == null ) {
      System.out.println("buildSettings not exists!");
      return;
    }
    // LastUpgradeCheck = 0630, CreatedOnToolsVersion = 6.3.1
    if ( buildSettingsDict.objectForKey("PRODUCT_BUNDLE_IDENTIFIER") != null ) {

        this.bundleIdentifier = buildSettingsDict.objectForKey("PRODUCT_BUNDLE_IDENTIFIER").toString();
    }
    else if ( buildSettingsDict.objectForKey("PRODUCT_NAME") != null ) {
        this.productName = buildSettingsDict.objectForKey("PRODUCT_NAME").toString();
    }
    this.infoPlistFile = buildSettingsDict.objectForKey("INFOPLIST_FILE").toString();
    //this.codeSignStyle = buildSettingsDict.objectForKey("CODE_SIGN_STYLE").toString();
    if ( buildSettingsDict.objectForKey("BUNDLE_LOADER") == null && buildSettingsDict.objectForKey("TEST_TARGET_NAME") == null && buildSettingsDict.objectForKey("DEVELOPMENT_TEAM") != null ) {
	this.developmentTeamId = buildSettingsDict.objectForKey("DEVELOPMENT_TEAM").toString();
    }
    if ( !automatic ) {
      this.provisioningProfileUUID = buildSettingsDict.objectForKey("PROVISIONING_PROFILE").toString();
      this.provisioningProfileSpecifier = buildSettingsDict.objectForKey("PROVISIONING_PROFILE_SPECIFIER").toString();
    }
    //else {
    //  this.codeSignStyle = "Automatic";
    //}
  }
}
