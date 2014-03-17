package au.com.rayh;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

/**
 * Created by olarivain on 3/16/14.
 */
public class XCodeBuilder extends AbstractXCodeBuilder
{
    @DataBoundConstructor
    public XCodeBuilder(Boolean buildIpa, Boolean generateArchive, Boolean cleanBeforeBuild, Boolean cleanTestReports, String configuration, String target, String sdk, String xcodeProjectPath, String xcodeProjectFile, String xcodebuildArguments, String embeddedProfileFile, String cfBundleVersionValue, String cfBundleShortVersionStringValue, Boolean unlockKeychain, String keychainName, String keychainPath, String keychainPwd, String symRoot, String xcodeWorkspaceFile, String xcodeSchema, String configurationBuildDir, String codeSigningIdentity, Boolean allowFailingBuildResults, String ipaName, Boolean provideApplicationVersion, String ipaOutputDirectory)
    {
        super(buildIpa, generateArchive, cleanBeforeBuild, cleanTestReports, configuration, target, sdk, xcodeProjectPath, xcodeProjectFile, xcodebuildArguments, embeddedProfileFile, cfBundleVersionValue, cfBundleShortVersionStringValue, unlockKeychain, keychainName, keychainPath, keychainPwd, symRoot, xcodeWorkspaceFile, xcodeSchema, configurationBuildDir, codeSigningIdentity, allowFailingBuildResults, ipaName, provideApplicationVersion, ipaOutputDirectory, false, null, null, false);
    }

    @Override
    public XCodeBuildDescriptor getDescriptor() {
        return (XCodeBuildDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class XCodeBuildDescriptor extends AbstractXCodeDescriptor
    {
        @Override
        public String getDisplayName() {
            return Messages.XCodeBuilder_xcode();
        }
    }
}
