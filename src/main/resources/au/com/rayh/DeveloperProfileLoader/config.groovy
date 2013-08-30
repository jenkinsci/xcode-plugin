package au.com.rayh.DeveloperProfileLoader

def f = namespace(lib.FormTagLib)

f.entry(title:_("Profile"), field:"profileId") {
    f.select()
}
