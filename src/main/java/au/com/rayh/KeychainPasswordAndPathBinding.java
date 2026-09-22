/*
 * The MIT License
 *
 *  Copyright (c) 2016, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package au.com.rayh;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kazuhide.t@linux-powered.com">Kazuhide Takahashi</a>
 */
public class KeychainPasswordAndPathBinding extends MultiBinding<KeychainPasswordAndPath> {

    public final static String DEFAULT_KEYCHAIN_PATH_VARIABLE_NAME = "KEYCHAIN_PATH";
    private final static String DEFAULT_PASSWORD_VARIABLE_NAME = "PASSWORD";
    private final static String DEFAULT_IN_SEARCH_PATH_VARIABLE_NAME = "IN_SEARCH_PATH";

    @NonNull
    private final String keychainPathVariable;
    @NonNull
    private final String passwordVariable;
    @NonNull
    private final String inSearchPathVariable;

    /**
     *
     * @param keychainPathVariable if {@code null}, {@value DEFAULT_KEYCHAIN_PATH_VARIABLE_NAME} will be used.
     * @param passwordVariable if {@code null}, {@value DEFAULT_PASSWORD_VARIABLE_NAME} will be used.
     * @param inSearchPathVariable if {@code null}, {@value DEFAULT_IN_SEARCH_PATH_VARIABLE_NAME} will be used.
     * @param credentialsId identifier which should be referenced when accessing the credentials from a job/pipeline.
     */
    @DataBoundConstructor
    public KeychainPasswordAndPathBinding(@Nullable String keychainPathVariable, @Nullable String passwordVariable, @Nullable String inSearchPathVariable, String credentialsId) {
        super(credentialsId);
        this.keychainPathVariable = StringUtils.defaultIfBlank(keychainPathVariable, DEFAULT_KEYCHAIN_PATH_VARIABLE_NAME);
        this.passwordVariable = StringUtils.defaultIfBlank(passwordVariable, DEFAULT_PASSWORD_VARIABLE_NAME);
        this.inSearchPathVariable = StringUtils.defaultIfBlank(inSearchPathVariable, DEFAULT_IN_SEARCH_PATH_VARIABLE_NAME);
    }

    @NonNull
    public String getKeychainPathVariable() {
        return keychainPathVariable;
    }

    @NonNull
    public String getPasswordVariable() {
        return passwordVariable;
    }

    @NonNull
    public String getInSearchPathVariable() {
        return inSearchPathVariable;
    }

    @Override
    protected Class<KeychainPasswordAndPath> type() {
        return KeychainPasswordAndPath.class;
    }

    @Override
    public MultiEnvironment bind(@NonNull Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        KeychainPasswordAndPath credential = getCredentials(build);
        Map<String,String> m = new HashMap<String,String>();
        m.put(keychainPathVariable, credential.getKeychainPath());
        m.put(passwordVariable, credential.getPassword().getPlainText());
        m.put(inSearchPathVariable, credential.getInSearchPath());
        return new MultiEnvironment(m);
    }

    @Override
    public Set<String> variables() {
        return new HashSet<String>(Arrays.asList(keychainPathVariable, passwordVariable, inSearchPathVariable));
    }

    @Extension
    public static class DescriptorImpl extends BindingDescriptor<KeychainPasswordAndPath> {

        @Override protected Class<KeychainPasswordAndPath> type() {
            return KeychainPasswordAndPath.class;
        }

        @Override public String getDisplayName() {
            return Messages.KeychainPasswordAndPath_DisplayName();
        }
    }

}
