/*
 * The MIT License
 *
 * Copyright (c) 2011-2012, CloudBees, Inc., Stephen Connolly.
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

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.PasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.util.Secret;

/**
 * Credentials that have an ID, description and macOS Keychain password and path.
 *
 */
@NameWith(value = KeychainPasswordAndPath.NameProvider.class, priority = 32)
public interface KeychainPasswordAndPath extends StandardCredentials, PasswordCredentials {
    /**
     *
     *
     */
    Secret getPassword();
    String getKeychainPath();
    boolean isInSearchPath();
    String getInSearchPath();

    /**
     * Our name provider.
     *
     * @since 2.0.12
     */
    public static class NameProvider extends CredentialsNameProvider<KeychainPasswordAndPath> {

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public String getName(@NonNull KeychainPasswordAndPath c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return c.getKeychainPath() + (description != null ? " (" + description + ")" : "");
        }
    }
}
