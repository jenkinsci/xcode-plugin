package au.com.rayh;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.Lists;

@SuppressWarnings("rawtypes")
public class OSXKeychainBuildWrapper extends BuildWrapper {
	@DataBoundConstructor
	public OSXKeychainBuildWrapper() {
		
	}
	
	@Override
	public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {
			
			public boolean tearDown(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
				listener.getLogger().println("[OS X] restore keychains as defined in global configuration");
				
				FilePath projectRoot = build.getWorkspace();
				EnvVars envs = build.getEnvironment(listener);

				List<String> commandLine = Lists.newArrayList("/usr/bin/security");
	            commandLine.add("list-keychains");
	            commandLine.add("-s");
	            
	            String defaultKeychainName = getDescriptor().getGlobalConfiguration().getDefaultKeychain();
	            Keychain defaultKeychain = null;
	            for (Keychain k : getDescriptor().getGlobalConfiguration().getKeychains()) {
	            	if (k.isInSearchPath() && ! StringUtils.isEmpty(k.getKeychainPath())) {
	            		commandLine.add(envs.expand(k.getKeychainPath()));
	            		
	            		if (defaultKeychain == null && defaultKeychainName != null && k.getKeychainName().equals(defaultKeychainName)) {
	            			defaultKeychain = k;
	            		}
	            	}
	            }

	            int returnCode = launcher.launch().envs(envs).cmds(commandLine).stdout(listener).pwd(projectRoot).join();
	            
	            // Set default keychain
	            if (returnCode == 0 && defaultKeychain != null) {
	            	returnCode = launcher.launch().envs(envs).cmds("/usr/bin/security", "default-keychain", "-d", "user", "-s", envs.expand(defaultKeychain.getKeychainPath())).stdout(listener).pwd(projectRoot).join();
	            }

	            // Something went wrong, mark unstable to ping user
	            if (returnCode > 0) {
	            	build.setResult(Result.UNSTABLE);
	            }
				
				// Do not interfere the build status
				return true;
			}
		};
	}
	
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {
		/**
		 * Obtain the global configuration
		 */
		@Inject
		private GlobalConfigurationImpl globalConfiguration; 
		
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.OSXKeychainBuildWrapper_restoreOSXKeychainsAfterBuildProcessAsDefinedInGlobalConfiguration();
		}
		
		public GlobalConfigurationImpl getGlobalConfiguration() {
			return globalConfiguration;
		}
	}
}
