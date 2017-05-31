package au.com.rayh;

import hudson.Extension;
import hudson.util.IOUtils;
import hudson.util.Secret;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import jenkins.security.ConfidentialKey;

import org.apache.commons.fileupload.FileItem;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

/**
 * Apple developer profile, which consists of any number of PKCS12 of the private key
 * and the certificate for code signing, and mobile provisioning profiles.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeveloperProfile extends BaseStandardCredentials {
    /**
     * Password of the PKCS12 files inside the profile.
     */
    private Secret password;


    @DataBoundConstructor
    public DeveloperProfile(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description,
            Secret password, FileItem image) throws IOException {
        super(scope, id, description);
        this.password= password;

        if (image!=null) {
            // for added secrecy, store this in the confidential store
            new ConfidentialKeyImpl(id).store(image);
        }
    }

    @Deprecated
    public DeveloperProfile(String id, String description, Secret password, FileItem image) throws IOException {
        this(CredentialsScope.GLOBAL,id,description,password,image);
    }

    public Secret getPassword() {
        return password;
    }

    /**
     * Retrieves the PKCS12 byte image.
     */
    public byte[] getImage() throws IOException {
        return new ConfidentialKeyImpl(getId()).load();
    }

    /**
     * Obtains the certificates in this developer profile.
     */
    public @Nonnull List<X509Certificate> getCertificates() throws IOException, GeneralSecurityException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(getImage()))) {
            List<X509Certificate> r = new ArrayList<>();

            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                if (ze.getName().endsWith(".p12")) {
                    KeyStore ks = KeyStore.getInstance("pkcs12");
                    ks.load(zip, password.getPlainText().toCharArray());
                    Enumeration<String> en = ks.aliases();
                    while (en.hasMoreElements()) {
                        String s = en.nextElement();
                        Certificate c = ks.getCertificate(s);
                        if (c instanceof X509Certificate) {
                            r.add((X509Certificate) c);
                        }
                    }
                }
            }

            return r;
        }
    }

    public String getDisplayNameOf(X509Certificate p) {
        String name = p.getSubjectDN().getName();
        try {
            LdapName n = new LdapName(name);
            for (Rdn rdn : n.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN"))
                    return rdn.getValue().toString();
            }
        } catch (InvalidNameException e) {
            // fall through
        }
        return name; // fallback
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Apple Developer Profile";
        }
    }

    static class ConfidentialKeyImpl extends ConfidentialKey {
        ConfidentialKeyImpl(String id) {
            super(DeveloperProfile.class.getName()+"."+id);
        }

        public void store(FileItem submitted) throws IOException {
            super.store(IOUtils.toByteArray(submitted.getInputStream()));
        }

        public @CheckForNull byte[] load() throws IOException {
            return super.load();
        }
    }
}
