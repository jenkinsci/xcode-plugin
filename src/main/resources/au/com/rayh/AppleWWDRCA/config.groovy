package au.com.rayh.AppleWWDRCA

import java.security.GeneralSecurityException
import java.security.cert.CertificateException

f = namespace(lib.FormTagLib)
st = namespace("jelly:stapler")

def fileForm() {
    f.entry(title:_("*.cer File"), field:"image") {
        raw("<input type=file name=image size=40 jsonAware=yes>")
    }
}

def img = instance?.image
if ( img == null ) {
    fileForm()
}
else {
    f.entry(title:_("Contents")) { // show the certificates in the .cer file
        try {
            def certs = instance.certificates
            certs.each { c ->
                boolean valid = true;
                try {
                    c.checkValidity();
                }
                catch ( CertificateException e ) {
                    valid = false;
                }

                div(class:valid ? null : 'error') {
                    text(instance.getDisplayNameOf(c))
                    if ( !valid )
                        text(_("expired"));
                }
            }
            if ( certs.isEmpty() )
                div(class:'error', _("There's no certificate in this .cer file"));
        }
        catch ( IOException e ) {
            div(class:'error', _("Not a valid certificates file:") + "${e.message}")
        }
        catch ( GeneralSecurityException e ) {
            div(class:'error', _("Not a valid certificates file:") + "${e.message}")
        }
    }
    f.optionalBlock( title:_("Re-upload *.cer File"), inline:true ) {
        fileForm()
    }
}

st.include(page:"id-and-description", class:descriptor.clazz)
