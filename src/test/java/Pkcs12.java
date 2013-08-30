import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * A test program to inspect PKCS12 file
 *
 * @author Kohsuke Kawaguchi
 */
public class Pkcs12 {
    public static void main(String[] args) throws Exception {
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(new FileInputStream(args[0]),args[1].toCharArray());
        Enumeration<String> en = ks.aliases();
        while (en.hasMoreElements()) {
            String s = en.nextElement();
            System.out.println(s);
            X509Certificate c = (X509Certificate)ks.getCertificate(s);
            if (c!=null) {
                X500Principal p = c.getSubjectX500Principal();
                System.out.println(p.getName());
                System.out.println(getCanonicalName(p));
            }
        }
    }

    private static String getCanonicalName(X500Principal p) throws InvalidNameException {
        LdapName n = new LdapName(p.getName());
        for (Rdn rdn : n.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN"))
                return rdn.getValue().toString();
        }
        return p.getName(); // fallback
    }
}
