/*
 * The MIT License
 *
 * Copyright (c) 2011-2016, CloudBees, Inc., Stephen Connolly.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.security.ACL;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.List;

/**
 *
 */
@SuppressWarnings("unused") // read resolved by extension plugins
public class KeychainPasswordAndPathImpl extends BaseStandardCredentials implements KeychainPasswordAndPath {

    /**
     * The Keychain path.
     */
    @NonNull
    private String keychainPath;

    /**
     * The password.
     */
    @NonNull
    private Secret password;

    /**
     * The flag for keychain in search path.
     */
    private String inSearchPath;

    /**
     * Constructor.
     *
     * @param scope           the credentials scope
     * @param id              the ID or {@code null} to generate a new one.
     * @param description     the description.
     * @param keychainPath    the Keychain path.
     * @param password        the password.
     * @param inSearchPath    flag for keychain in search path.
     */
    @DataBoundConstructor
    @SuppressWarnings("unused") // by stapler
    public KeychainPasswordAndPathImpl(@CheckForNull CredentialsScope scope,
                                       @CheckForNull String id,
                                       @CheckForNull String description,
                                       @CheckForNull String keychainPath,
                                       @CheckForNull String password,
                                       @CheckForNull String inSearchPath) {
        super(scope, id, description);
        this.keychainPath = Util.fixNull(keychainPath);
        this.password = Secret.fromString(password);
        this.inSearchPath = inSearchPath;
    }

    /**
     * {@inheritDoc}
     */
    public Secret getPassword() {
        return password;
    }

    @DataBoundSetter
    public void setPassword(Secret password) {
        this.password = password;
    }

    /**
     * macOS Keychain path.
     * @return full path for macOS keychain
     */
    @NonNull
    public String getKeychainPath() {
        return keychainPath;
    }

    @DataBoundSetter
    public void setKeychainPath(String keychainPath) {
        this.keychainPath = keychainPath;
    }

    /**
     * Add keychain to search path.
     * @return inSearchPath by String.
     */
    public String getInSearchPath() {
        return inSearchPath;
    }

    @DataBoundSetter
    public void setInSearchPath(String inSearchPath) {
        this.inSearchPath = inSearchPath;
    }

    /**
     * Add keyc hain to search path.
     * @return check is inSearchPath equals "true".
     */
    public boolean isInSearchPath() {
        return inSearchPath == null ? false : inSearchPath.equals("true");
    }

    /**
     * {@inheritDoc}
     */
    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.KeychainPasswordAndPath_DisplayName();
        }

        /*
         * {@inheritDoc}
         *
        @Override
        public String getIconClassName() {
            return "icon-credentials";
        }
        */
    }

    public static List<KeychainPasswordAndPathImpl> getAllKeycainInfo() {
        return CredentialsProvider.lookupCredentials(KeychainPasswordAndPathImpl.class, (hudson.model.Item)null, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
    }

}
