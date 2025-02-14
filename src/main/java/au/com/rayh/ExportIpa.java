package au.com.rayh;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.inject.Inject;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class ExportIpa extends Builder implements SimpleBuildStep {
    private static final String[] VALID_IPA_EXPORT_METHODS = { "development", "ad-hoc", "enterprise", "app-store" };

    @CheckForNull
    private String xcodeProjectPath;
    @CheckForNull
    private String xcodeProjectFile;
    @CheckForNull
    private Boolean unlockKeychain;
    @Deprecated
    @CheckForNull
    private String keychainName;
    @CheckForNull
    private String keychainPath;
    @CheckForNull
    private Secret keychainPwd;
    @CheckForNull
    private String symRoot;
    @CheckForNull
    private String xcodeWorkspaceFile;
    @CheckForNull
    private String xcodeSchema;
    @CheckForNull
    private String archiveDir;
    @CheckForNull
    private String developmentTeamName;
    @CheckForNull
    private String developmentTeamID;
    @CheckForNull
    private String ipaName;
    @CheckForNull
    private String ipaOutputDirectory;
    @CheckForNull
    private String ipaExportMethod;
    /**
     * @deprecated 2.0.7
     *
    @CheckForNull
    private Boolean manualSigning;
     */
    @CheckForNull
    private String signingMethod;
    @CheckForNull
    private ArrayList<ProvisioningProfile> provisioningProfiles;
    @CheckForNull
    private String xcodeName;
    @CheckForNull
    private Boolean uploadBitcode;
    @CheckForNull
    private Boolean uploadSymbols;
    @CheckForNull
    private Boolean compileBitcode;
    @CheckForNull
    private String thinning;
    @CheckForNull
    private Boolean embedOnDemandResourcesAssetPacksInBundle;
    @CheckForNull
    private String onDemandResourcesAssetPacksBaseURL;
    @CheckForNull
    private String appURL;
    @CheckForNull
    private String displayImageURL;
    @CheckForNull
    private String fullSizeImageURL;
    @CheckForNull
    private String assetPackManifestURL;
    /**
     * @since 2.0.5
     */
    @CheckForNull
    private Boolean stripSwiftSymbols;
    /**
     * @since 2.0.7
     */
    @CheckForNull
    private Boolean copyProvisioningProfile;
    /*
     * @since 2.0.13
     */
    @CheckForNull
    private String keychainId;

    @CheckForNull
    public String getXcodeProjectPath() {
	return xcodeProjectPath;
    }

    @DataBoundSetter
    public void setXcodeProjectPath(String xcodeProjectPath) {
	this.xcodeProjectPath = xcodeProjectPath;
    }

    @CheckForNull
    public String getXcodeProjectFile() {
	return xcodeProjectFile;
    }

    public Boolean getUnlockKeychain() {
	return unlockKeychain;
    }

    @DataBoundSetter
    public void setUnlockKeychain(Boolean unlockKeychain) {
	this.unlockKeychain = unlockKeychain;
    }

    @CheckForNull
    public String getKeychainName() {
	return keychainName;
    }

    @DataBoundSetter
    public void setKeychainName(String keychainName) {
	this.keychainName = keychainName;
    }

    @CheckForNull
    public String getKeychainPath() {
	return keychainPath;
    }

    @DataBoundSetter
    public void setKeychainPath(String keychainPath) {
	this.keychainPath = keychainPath;
    }

    @CheckForNull
    public Secret getKeychainPwd() {
	return keychainPwd;
    }

    @DataBoundSetter
    public void setKeychainPwd(Secret keychainPwd) {
	this.keychainPwd = keychainPwd;
    }

    @CheckForNull
    public String getSymRoot() {
	return symRoot;
    }

    @DataBoundSetter
    public void setSymRoot(String symRoot) {
	this.symRoot = symRoot;
    }

    @CheckForNull
    public String getXcodeWorkspaceFile() {
	return xcodeWorkspaceFile;
    }

    @DataBoundSetter
    public void setXcodeWorkspaceFile(String xcodeWorkspaceFile) {
	this.xcodeWorkspaceFile = xcodeWorkspaceFile;
    }

    @CheckForNull
    public String getXcodeSchema() {
	return xcodeSchema;
    }

    @DataBoundSetter
    public void setXcodeSchema(String xcodeSchema) {
	this.xcodeSchema = xcodeSchema;
    }

    @CheckForNull
    public String getArchiveDir() {
	return archiveDir;
    }

    @DataBoundSetter
    public void setArchiveDir(String archiveDir) {
	this.archiveDir = archiveDir;
    }

    @CheckForNull
    public String getDevelopmentTeamName() {
	return developmentTeamName;
    }

    @DataBoundSetter
    public void setDevelopmentTeamName(String developmentTeamName) {
	this.developmentTeamName = developmentTeamName;
    }

    @CheckForNull
    public String getDevelopmentTeamID() {
	return developmentTeamID;
    }

    @DataBoundSetter
    public void setDevelopmentTeamID(String developmentTeamID) {
	this.developmentTeamID = developmentTeamID;
    }

    @CheckForNull
    public String getIpaName() {
	return ipaName;
    }

    @DataBoundSetter
    public void setIpaName(String ipaName) {
	this.ipaName = ipaName;
    }

    @CheckForNull
    public String getIpaOutputDirectory() {
	return ipaOutputDirectory;
    }

    @DataBoundSetter
    public void setIpaOutputDirectory(String ipaOutputDirectory) {
	this.ipaOutputDirectory = ipaOutputDirectory;
    }

    @CheckForNull
    public String getIpaExportMethod() {
	return ipaExportMethod;
    }

    @DataBoundSetter
    public void setIpaExportMethod(String ipaExportMethod) {
	this.ipaExportMethod = ipaExportMethod;
    }

    @Deprecated
    public Boolean getManualSigning() {
	return ( signingMethod == null || signingMethod.equals("manual") );
    }

    @Deprecated
    @DataBoundSetter
    public void setManualSigning(Boolean manualSigning) {
	this.signingMethod = BooleanUtils.isTrue(manualSigning) ? "manual" : "automatic";
    }

    @CheckForNull
    public String getSigningMethod() {
	return signingMethod == null ? "automatic" : signingMethod;
    }

    @DataBoundSetter
    public void setSigningMethod(String signingMethod) {
	this.signingMethod = signingMethod;
    }

    @CheckForNull
    public ArrayList<ProvisioningProfile> getProvisioningProfiles() {
	return provisioningProfiles;
    }

    @DataBoundSetter
    public void setProvisioningProfiles(ArrayList<ProvisioningProfile> provisioningProfiles) {
	this.provisioningProfiles = provisioningProfiles;
    }

    @CheckForNull
    public String getXcodeName() {
	return xcodeName;
    }

    @DataBoundSetter
    public void setXcodeName(String xcodeName) {
	this.xcodeName = xcodeName;
    }

    public Boolean getUploadBitcode() {
	return uploadBitcode;
    }

    @DataBoundSetter
    public void setUploadBitcode(Boolean uploadBitcode) {
	this.uploadBitcode = uploadBitcode;
    }

    public Boolean getUploadSymbols() {
	return uploadSymbols;
    }

    @DataBoundSetter
    public void setUploadSymbols(Boolean uploadSymbols) {
	this.uploadSymbols = uploadSymbols;
    }

    public Boolean getCompileBitcode() {
	return compileBitcode;
    }

    @DataBoundSetter
    public void setCompileBitcode(Boolean compileBitcode) {
	this.compileBitcode = compileBitcode;
    }

    @CheckForNull
    public String getThinning() {
	return thinning;
    }

    @DataBoundSetter
    public void setThinning(String thinning) {
	this.thinning = thinning;
    }

    public Boolean getPackResourcesAsset() {
	return embedOnDemandResourcesAssetPacksInBundle;
    }

    @DataBoundSetter
    public void setPackResourcesAsset(Boolean packResourcesAsset) {
	this.embedOnDemandResourcesAssetPacksInBundle = packResourcesAsset;
    }

    @CheckForNull
    public String getResourcesAssetURL() {
	return onDemandResourcesAssetPacksBaseURL;
    }

    @DataBoundSetter
    public void setResourcesAssetURL(String resourcesAssetURL) {
	this.onDemandResourcesAssetPacksBaseURL = resourcesAssetURL;
    }

    @CheckForNull
    public String getAppURL() {
	return appURL;
    }

    @DataBoundSetter
    public void setAppURL(String appURL) {
	this.appURL = appURL;
    }

    @CheckForNull
    public String getDisplayImageURL() {
	return displayImageURL;
    }

    @DataBoundSetter
    public void setDisplayImageURL(String displayImageURL) {
	this.displayImageURL = displayImageURL;
    }

    @CheckForNull
    public String getFullSizeImageURL() {
	return fullSizeImageURL;
    }

    @DataBoundSetter
    public void setFullSizeImageURL(String fullSizeImageURL) {
	this.fullSizeImageURL = fullSizeImageURL;
    }

    @CheckForNull
    public String getAssetPackManifestURL() {
	return assetPackManifestURL;
    }

    @DataBoundSetter
    public void setAssetPackManifestURL(String assetPackManifestURL) {
	this.assetPackManifestURL = assetPackManifestURL;
    }

    public Boolean getStripSwiftSymbols() {
	return stripSwiftSymbols == null ? Boolean.valueOf(true) : stripSwiftSymbols;
    }

    @DataBoundSetter
    public void setStripSwiftSymbols(Boolean stripSwiftSymbols) {
	this.stripSwiftSymbols = stripSwiftSymbols;
    }

    public Boolean getCopyProvisioningProfile() {
	return copyProvisioningProfile == null ? Boolean.valueOf(true) : copyProvisioningProfile;
    }

    @DataBoundSetter
    public void setCopyProvisioningProfile(Boolean copyProvisioningProfile) {
	this.copyProvisioningProfile = copyProvisioningProfile;
    }

    @CheckForNull
    public String getKeychainId() {
	return keychainId;
    }

    @DataBoundSetter
    public void setKeychainId(String keychainId) {
	this.keychainId = keychainId;
    }

    @DataBoundConstructor
    public ExportIpa() {
    }

    @Deprecated
    public ExportIpa(String xcodeProjectPath, String xcodeProjectFile,
                Boolean unlockKeychain, String keychainName, String keychainPath, String keychainPwd, String symRoot, String xcodeWorkspaceFile,
                String xcodeSchema, String archiveDir, String developmentTeamName, String developmentTeamID,
                String ipaName, String ipaOutputDirectory,
                String ipaExportMethod,
                String signingMethod, ArrayList<ProvisioningProfile> provisioningProfiles, String xcodeName,
		Boolean uploadBitcode, Boolean uploadSymbols, Boolean compileBitcode, String thinning,
		Boolean packResourcesAsset, String resourcesAssetURL,
		String appURL, String displayImageURL, String fullSizeImageURL,
		String assetPackManifestURL) {
	this();
        this.xcodeProjectPath = xcodeProjectPath;
        this.xcodeProjectFile = xcodeProjectFile;
        this.keychainName = keychainName;
        this.xcodeWorkspaceFile = xcodeWorkspaceFile;
        this.xcodeSchema = xcodeSchema;
        this.developmentTeamName = developmentTeamName;
        this.developmentTeamID = developmentTeamID;
        this.unlockKeychain = unlockKeychain;
        this.keychainPath = keychainPath;
        this.keychainPwd = Secret.fromString(keychainPwd);
        this.symRoot = symRoot;
        this.archiveDir = archiveDir;
        this.ipaName = ipaName;
        this.ipaOutputDirectory = ipaOutputDirectory;
        this.ipaExportMethod = ipaExportMethod;
        this.signingMethod = signingMethod;
        this.provisioningProfiles = provisioningProfiles;
	this.xcodeName = xcodeName;
        this.uploadBitcode = uploadBitcode;
        this.uploadSymbols = uploadSymbols;
        this.compileBitcode = compileBitcode;
        this.thinning = thinning;
        this.embedOnDemandResourcesAssetPacksInBundle = packResourcesAsset;
        this.onDemandResourcesAssetPacksBaseURL = resourcesAssetURL;
        this.appURL = appURL;
        this.displayImageURL = displayImageURL;
        this.fullSizeImageURL = fullSizeImageURL;
        this.assetPackManifestURL = assetPackManifestURL;
	this.stripSwiftSymbols = true;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath filePath, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        _perform(build, filePath, launcher, build.getEnvironment(listener), listener);
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private boolean _perform(Run<?,?> build, FilePath filePath, Launcher launcher, EnvVars envs, TaskListener listener) throws InterruptedException, IOException {
	XCodeBuilder builder = new XCodeBuilder(true, false, true, null, false, false, null,
                null, null, xcodeProjectPath, xcodeProjectFile, null,
                null, null, unlockKeychain,
                keychainName, keychainPath, Secret.toString(keychainPwd), symRoot, xcodeWorkspaceFile,
                xcodeSchema, archiveDir, developmentTeamName, developmentTeamID, false,
                ipaName, false, ipaOutputDirectory, false, null,
                null, false, ipaExportMethod, signingMethod, provisioningProfiles, xcodeName,
		uploadBitcode, uploadSymbols, compileBitcode, thinning,
		embedOnDemandResourcesAssetPacksInBundle, onDemandResourcesAssetPacksBaseURL,
		appURL, displayImageURL, fullSizeImageURL, assetPackManifestURL);
	builder.setStripSwiftSymbols(stripSwiftSymbols);
	builder.setCopyProvisioningProfile(copyProvisioningProfile);
		
	builder.setSkipBuildStep(true);
	builder.setKeychainId(keychainId);
	builder.perform(build, filePath, launcher, listener);
	return true;
    }

    public GlobalConfigurationImpl getGlobalConfiguration() {
        return getDescriptor().getGlobalConfiguration();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("exportIpa")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        GlobalConfigurationImpl globalConfiguration;

        @Override
        public String getDisplayName() {
            return Messages.ExportIpa_ExportIpa();
        }

        public DescriptorImpl() {
            load();
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        @Inject
        void setGlobalConfiguration(GlobalConfigurationImpl c) {
            this.globalConfiguration = c;
        }

        public GlobalConfigurationImpl getGlobalConfiguration() {
            return globalConfiguration;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getUUID() {
            return "" + UUID.randomUUID().getMostSignificantBits();
        }

	public FormValidation doCheckArchiveDir(@QueryParameter String value) {
	    if ( value.isEmpty() ) {
		return FormValidation.error(Messages.ExportIpa_NeedToSpecifyArchiveLocation());
	    }
	    return FormValidation.ok();
	}

        public FormValidation doCheckResourcesAssetURL(@QueryParameter String value, @QueryParameter Boolean packResourcesAsset) {
            if ( StringUtils.isEmpty(value) && !packResourcesAsset ) {
                return FormValidation.error(Messages.XCodeBuilder_NeedOnDemandResourcesURL());
            }
            return FormValidation.ok();
        }

	public FormValidation doCheckIpaExportMethod(@QueryParameter String value) {
	    if ( !ArrayUtils.contains(VALID_IPA_EXPORT_METHODS, value) ) {
		String validMethodsMsg = StringUtils.join(VALID_IPA_EXPORT_METHODS, ", ");
		return FormValidation.error(Messages.XCodeBuilder_IpaExportMethodMuestBeOneOfTheFollowing(validMethodsMsg));
	    }
	    return FormValidation.ok();
	}

	public FormValidation doCheckXcodeWorkspaceFile(@QueryParameter String value, @QueryParameter String xcodeSchema, @QueryParameter String target) {
	    if ( !StringUtils.isEmpty(value) ) {
		if ( !StringUtils.isEmpty(target) ) {
		    return FormValidation.error(Messages.XCodeBuilder_WorkspaceAndTargetCantSpecifySameTime());
		}
		if ( StringUtils.isEmpty(xcodeSchema) ) {
		    return FormValidation.error(Messages.XCodeBuilder_SpecifyWorkspaceAlsoSetScheme());
		}
	    }
	    return FormValidation.ok();
	}

	public FormValidation doCheckXcodeSchema(@QueryParameter String value,  @QueryParameter Boolean generateArchive, @QueryParameter String xcodeWorkspaceFile, @QueryParameter String target) {
	    if ( !StringUtils.isEmpty(value) ) {
		if ( !StringUtils.isEmpty(target) ) {
		    return FormValidation.error(Messages.XCodeBuilder_SchemeAndTargetCantSpecifySameTime());
		}
	    }
	    else {
		if ( !StringUtils.isEmpty(xcodeWorkspaceFile) ) {
		    return FormValidation.error(Messages.XCodeBuilder_SpecifyWorkspaceAlsoSetScheme());
		}
                return FormValidation.warning(Messages.XCodeBuilder_NeedSchema());
	    }
	    return FormValidation.ok();
	}
    }
}
