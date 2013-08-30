import hudson.util.IOUtils;
import org.bouncycastle.cms.CMSSignedData;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class Pkcs7 {
    public static void main(String[] args) throws Exception {
        CMSSignedData cms = new CMSSignedData(IOUtils.toByteArray(new FileInputStream(args[0])));
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        cms.getSignedContent().write(payload);

        // mobile provisioning profile has a payload of plist
        System.out.write(payload.toByteArray());
    }
}
