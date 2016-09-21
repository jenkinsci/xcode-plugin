package au.com.rayh.DeveloperProfile

import java.security.GeneralSecurityException
import java.security.cert.CertificateException

f = namespace(lib.FormTagLib)
st = namespace("jelly:stapler")

def fileForm() {
    f.entry(title:_("*.developerprofile  File"), field:"image") {
        raw("<input type=file name=image size=40 jsonAware=yes>")
    }
    f.entry(title:_("Password"), field:"password") {
        f.password()
    }
}

def img = instance?.image
if (img ==null) {
    fileForm()
} else {
    f.entry(title:_("Contents")) {// show the certificates in the profile
        try {
            def certs = instance.certificates
            certs.each { c ->
                boolean valid=true;
                try {
                    c.checkValidity();
                } catch (CertificateException e) {
                    valid = false;
                }

                div(class:valid?null:'error') {
                    text(instance.getDisplayNameOf(c))
                    if (!valid)
                        text("expired");
                }
            }
            if (certs.isEmpty())
                div(class:'error', "There's no certificate in this profile");
        } catch (IOException e) {
            div(class:'error', "Not a developer profile or a wrong password: ${e.message}")
        } catch (GeneralSecurityException e) {
            div(class:'error', "Not a developer profile or a wrong password: ${e.message}")
        }
    }
    f.optionalBlock(title:_("Re-upload *.developerprofile File"),inline:true) {
        fileForm()
    }
}

st.include(page: "id-and-description", class: descriptor.clazz)
