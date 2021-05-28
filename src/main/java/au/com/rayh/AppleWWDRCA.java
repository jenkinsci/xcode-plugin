package au.com.rayh;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.security.ACL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import jenkins.security.ConfidentialKey;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

/**
 * Apple Worldwide Developer Relations Certification Authority, which consists of any number of
 * certificate for prepare certificate for code signing, and mobile provisioning profiles.
 *
 * @author Kazuhide Takahashi
 */
public class AppleWWDRCA extends BaseStandardCredentials {
    @DataBoundConstructor
    public AppleWWDRCA(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description,
            FileItem image) throws IOException {
        super(scope, id, description);

        if ( image != null ) {
            // for added secrecy, store this in the confidential store
            new ConfidentialKeyImpl(id).store(image);
        }
    }

    /**
     * Retrieves the AppleWWDRCA.cer file image.
     * @return AppleWWDRCA.cer file image
     * @throws IOException file I/O
     */
    public byte[] getImage() throws IOException {
        return new ConfidentialKeyImpl(getId()).load();
    }

    /**
     * Obtains the certificates in this AppleWWDRCA.cer file.
     * @return X509Certificates
     * @throws IOException file I/O
     * @throws GeneralSecurityException Certificate error
     */
    public @Nonnull List<X509Certificate> getCertificates() throws IOException, GeneralSecurityException {
        List<X509Certificate> r = new ArrayList<>();
        try ( InputStream inputStream = new ByteArrayInputStream(getImage()) ) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection c = certificateFactory.generateCertificates(inputStream);
            Iterator i = c.iterator();
            while ( i.hasNext() ) {
                X509Certificate cert = (X509Certificate) i.next();
                r.add((X509Certificate)cert);
            }
        }
        return r;
     }

    public String getDisplayNameOf(X509Certificate p) {
        String name = p.getSubjectDN().getName();
        try {
            LdapName n = new LdapName(name);
            for ( Rdn rdn : n.getRdns() ) {
                if ( rdn.getType().equalsIgnoreCase("CN") )
                    return rdn.getValue().toString();
            }
        }
        catch ( InvalidNameException e ) {
            // fall through
        }
        return name; // fallback
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Apple Worldwide Developer Relations Certification Authority";
        }
    }

    static class ConfidentialKeyImpl extends ConfidentialKey {
        ConfidentialKeyImpl(String id) {
            super(AppleWWDRCA.class.getName()+"."+id);
        }

        public void store(FileItem submitted) throws IOException {
            super.store(IOUtils.toByteArray(submitted.getInputStream()));
        }

        public @CheckForNull byte[] load() throws IOException {
            return super.load();
        }
    }

    public static List<AppleWWDRCA> getAllCertificates() {
	    return CredentialsProvider.lookupCredentials(AppleWWDRCA.class, (hudson.model.Item)null, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
    }
}
