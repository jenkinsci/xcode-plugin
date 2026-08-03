package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collections;

/**
 * @author <a href="mailto:kazuhide.t@linux-powered.com">Kazuhide Takahashi</a>
 */
public class XcodeCredentialsHelper {

    private XcodeCredentialsHelper() {}

    @CheckForNull
    public static KeychainPasswordAndPath getCredentials(String credentialsId, ItemGroup context) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return (KeychainPasswordAndPath) CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(KeychainPasswordAndPath.class, context,
                        ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(credentialsId));
    }

    public static ListBoxModel doFillCredentialsIdItems(ItemGroup context) {
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                        CredentialsMatchers.always(),
                        CredentialsProvider.lookupCredentials(KeychainPasswordAndPath.class,
                                context,
                                ACL.SYSTEM,
                                Collections.EMPTY_LIST));
    }
}
