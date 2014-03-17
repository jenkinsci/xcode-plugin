package au.com.rayh;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.List;

import hudson.Extension;
import hudson.util.ListBoxModel;

/**
 * Created by olarivain on 3/16/14.
 */
public class XCodeTester extends AbstractXCodeBuilder
{

    @DataBoundConstructor
    public XCodeTester(Boolean cleanBeforeBuild, String configuration, String target, String sdk, String xcodeProjectPath, String xcodeProjectFile, String xcodebuildArguments, String cfBundleVersionValue, String cfBundleShortVersionStringValue, String symRoot, String xcodeWorkspaceFile, String xcodeSchema, String configurationBuildDir, Boolean allowFailingBuildResults, Boolean provideApplicationVersion, String testOsVersion, String testDevice, Boolean resetSimulator)
    {
        super(false, false, cleanBeforeBuild, false, configuration, target, sdk, xcodeProjectPath, xcodeProjectFile, xcodebuildArguments, null, cfBundleVersionValue, cfBundleShortVersionStringValue, false, null, null, null, symRoot, xcodeWorkspaceFile, xcodeSchema, configurationBuildDir, null, allowFailingBuildResults, null, provideApplicationVersion, null, true, testOsVersion, testDevice, resetSimulator);
    }

    @Override
    public XCodeTestDescriptor getDescriptor() {
        XCodeTestDescriptor descriptor = (XCodeTestDescriptor) super.getDescriptor();
        descriptor.testDevice = testDevice;
        descriptor.testOsVersion = testOsVersion;

        return descriptor;
    }

    @Extension
    public static final class XCodeTestDescriptor extends AbstractXCodeDescriptor
    {
        private String testOsVersion;

        private String testDevice;

        @Override
        public String getDisplayName() {
            return Messages.XCodeBuilder_xcodeTest();
        }

        public ListBoxModel doFillTestDeviceItems() {
            ListBoxModel items = new ListBoxModel();
            for (String simulator : getSupportedSimulators()) {
                ListBoxModel.Option option = new ListBoxModel.Option(simulator, simulator);
                option.selected = StringUtils.equals(testDevice, simulator);
                items.add(option);
            }
            return items;
        }

        public ListBoxModel doFillTestOsVersionItems() {
            ListBoxModel items = new ListBoxModel();
            for (String osVersion : getSupportedOsVersions()) {
                ListBoxModel.Option option = new ListBoxModel.Option(osVersion, osVersion);
                option.selected = StringUtils.equals(testOsVersion, osVersion);
                items.add(option);
            }
            return items;
        }

        private List<String> getSupportedSimulators()
        {
            return Arrays.asList(new String[]{"iPhone Retina (3.5-inch)", "iPhone Retina (4-inch)",
                    "iPhone Retina (4-inch 64-bit)", "iPad", "iPad Retina", "iPad Retina (64-bit)"});
        }

        private List<String> getSupportedOsVersions()
        {
            return Arrays.asList(new String[]{"6.0", "6.1", "7.0", "7.1"});
        }
    }
}
