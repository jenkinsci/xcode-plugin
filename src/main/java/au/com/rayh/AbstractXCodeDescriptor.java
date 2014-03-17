package au.com.rayh;

import java.util.ArrayList;
import java.util.UUID;

import javax.inject.Inject;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.CopyOnWriteList;

/**
 * Created by olarivain on 3/16/14.
 */
public abstract class AbstractXCodeDescriptor extends BuildStepDescriptor<Builder>
{
    GlobalConfigurationImpl globalConfiguration;

    // backward compatibility
    @Deprecated
    private transient String xcodebuildPath;
    private transient String agvtoolPath;
    private transient String xcrunPath;
    private transient CopyOnWriteList<Keychain> keychains;

    public AbstractXCodeDescriptor() {
        load();
    }

    @Inject
    void setGlobalConfiguration(GlobalConfigurationImpl c) {
        this.globalConfiguration = c;
        {// data migration from old format
            boolean modified = false;
            if (xcodebuildPath!=null) {
                c.setXcodebuildPath(xcodebuildPath);
                modified = true;
            }
            if (agvtoolPath!=null) {
                c.setAgvtoolPath(agvtoolPath);
                modified = true;
            }
            if (xcrunPath!=null) {
                c.setXcrunPath(xcrunPath);
                modified = true;
            }
            if (keychains!=null) {
                c.setKeychains(new ArrayList<Keychain>(keychains.getView()));
                modified = true;
            }
            if (modified) {
                c.save();
                save(); // delete the old values from the disk now that the new values are committed
            }
        }
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    public GlobalConfigurationImpl getGlobalConfiguration() {
        return globalConfiguration;
    }

    public String getUUID() {
        return "" + UUID.randomUUID().getMostSignificantBits();
    }
}