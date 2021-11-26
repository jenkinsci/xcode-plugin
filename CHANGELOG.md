Changelog
===

### Newer versions

See [GitHub releases](https://github.com/jenkinsci/xcode-plugin/releases)

#### Version 2.0.15 (14th Mar 2021)
-   ##### Moved information in the AppleWWDRCA.cer file from the local file system to credentials.

#### Version 2.0.14 (14th Nov 2019)
-   ##### Fix backward compatibility problem at Import Developer Profile.

#### Version 2.0.13 (14th Nov 2019)
-   ##### Information to access the macOS keychain has been moved to credentials.
-   Fixed a bug in the project parser when multiple projects are in the workspace. ([JENKINS-59523](https://issues.jenkins-ci.org/browse/JENKINS-59523), [JENKINS-59609](https://issues.jenkins-ci.org/browse/JENKINS-59609))

#### Version 2.0.12 (27th May 2019)

-   ##### Allow to unlock/lock keychain on demand. ([JENKINS-56909](https://issues.jenkins-ci.org/browse/JENKINS-56909))

-   Modified to encrypt the keychain password. ([PR
    \#102](https://github.com/jenkinsci/xcode-plugin/pull/102))
-   Fix display bug developer profile credentials and more. ([PR
    \#103](https://github.com/jenkinsci/xcode-plugin/pull/103))

#### Version 2.0.11 (19th Feb 2019)

-   ##### Added a function to obtain the status of the test from the outline of the test result 'TestSummaries.plist'.

#### Version 2.0.10 (19th Nov 2018)

-   ##### Fix broken Xcode Project Parser. ([JENKINS-54414](http://JENKINS-54414), [JENKINS-54113](https://issues.jenkins-ci.org/browse/JENKINS-54113))

#### Version 2.0.9 (2nd Nov 2018)

-   ##### Added a option to use the 'Legacy Build System' instead of 'New Builld System' which became available from Xcode 9.

#### Version 2.0.8 (10th Oct 2018)

-   Corresponds when there is no DEVELOPMENT\_TEAM entry in the old
    Xcode project.
-   Delete unnecessary error messages.
-   When copying the provisioning profile from the project location, it
    needs to be done before Xcode compilation, so it is fixed.
-   Fixed a bug that error message got mixed when parsing provisioning
    profile and analysis failed.

#### Version 2.0.7 (20th Sep 2018)

-   Added the ability to copy provisioning profile files to manual code
    signing.
-   Change the help for importing developer profile to more detailed
    one.
-   Enhance backward compatibility.
-   Added a function to retrieve information necessary for CodeSign from
    Xcode project.

#### Version 2.0.6 (16th Aug 2018)

-   Fixed a bug that key chain was not unlocked properly when importing
    developer profile to keychain.
-   Added input validation when setting up developer profile loader.
-   Added input validation when setting up export IPA.
-   Fix some bugs.

#### Version 2.0.5 (9th Aug 2018)

-   More compatibility for Pipeline.
-   Added function to import developer profile into existing keychain.
-   Added a function to set exportOptions.plist an option for deleting
    Swift symbols when exporting IPA.

#### Version 2.0.4 (22th Jun 2018)

-   I will display an error message if the developer profile is not
    loaded.

#### Version 2.0.3 (18th Jun 2018)

-   Implemented the ability to export IPA files from already compiled
    archives.
-   [JENKINS-50266](https://issues.jenkins-ci.org/browse/JENKINS-50266)
    : Fix custom xcodebuild arguments not passed through to export
    archive step.
-   [JENKINS-51418](https://issues.jenkins-ci.org/browse/JENKINS-51418)
    : Fix to always be able to use developer team ID as a parameter
    regardless of automatic code signature.
-   Fix the Plugin is abnormally terminate if the out of order of lines
    from Xcodebuild output is changed.
-   Added a function to retrieve necessary information for code
    signature from compiled archive.
-   Support multiple versions of Xcode without using 'EnvInject Plugin'.

#### Version 2.0.2 (30th Apr 2018)

-   We released again due to infrastructure related problems, but the
    contents are the same as Ver 2.0.1

#### Version 2.0.1 (26th Apr 2018)

-   Supports Xcode 9 ([PR
    \#86](https://github.com/jenkinsci/xcode-plugin/pull/86),
    [PR\#87](https://github.com/jenkinsci/xcode-plugin/pull/87), [JENKINS-47744](https://issues.jenkins-ci.org/browse/JENKINS-45509), [JENKINS-45509](https://issues.jenkins-ci.org/browse/JENKINS-45509))
-   Adding Pipeline support for importDeveloperProfile and adding
    symbols ([PR
    \#89](https://github.com/jenkinsci/xcode-plugin/pull/89))

#### Version 2.0.0 (24th May 2017)

-    The plugin now requires Java 7 and Jenkins core \>= 1.625.1

-    The plugin now requires Xcode \>= 7

-   New : Supports Xcode 7/8
    ([PR\#76](https://github.com/jenkinsci/xcode-plugin/pull/76),
    [PR\#78](https://github.com/jenkinsci/xcode-plugin/pull/78), [JENKINS-44203](https://issues.jenkins-ci.org/browse/JENKINS-44203){.issue-link}, [JENKINS-44151](https://issues.jenkins-ci.org/browse/JENKINS-44151){.issue-link}, [JENKINS-43226](https://issues.jenkins-ci.org/browse/JENKINS-43226){.issue-link}, [JENKINS-43163](https://issues.jenkins-ci.org/browse/JENKINS-43163){.issue-link}, [JENKINS-38799](https://issues.jenkins-ci.org/browse/JENKINS-38799){.issue-link}, [JENKINS-38777](https://issues.jenkins-ci.org/browse/JENKINS-38777){.issue-link},
    [JENKINS-34307](https://issues.jenkins-ci.org/browse/JENKINS-34307){.issue-link}) 
    -   Replace 'xcrun PackageApplication' by 'xcodebuild
        -exportArchive' to package ipa

    -   New Development Team parameter which can be configured globally
        in Jenkins Global settings or locally at the project level

-   New : Adds regex for parsing UI test failures (for Xcode 8)
    ([PR\#75](https://github.com/jenkinsci/xcode-plugin/pull/75), [JENKINS-40938](https://issues.jenkins-ci.org/browse/JENKINS-40938){.issue-link})

-   New : Adds support
    ([PR\#74](https://github.com/jenkinsci/xcode-plugin/pull/74), [JENKINS-42457](https://issues.jenkins-ci.org/browse/JENKINS-42457){.issue-link}, [JENKINS-33355](https://issues.jenkins-ci.org/browse/JENKINS-33355){.issue-link})

#### Version 1.4.11 (21th September 2016)

Because XXXX happens

-   Fix: Support configurable ID for developer profile (JENKINS-32987)

#### Version 1.4.10 (20th September 2016)

-   New: Support configurable ID for developer profile (JENKINS-32987)
-   New: Allow the option not to sign the IPA with xcrun (JENKINS-32370)
-   Fix: use textarea for Xcode Build Arguments (JENKINS-30228)
-   Fix: Generating an archive builds the project twice (JENKINS-30362)
-   Fix: Allowed the use of environment variables in plist url
    (JENKINS-27236)

#### Version 1.4.9 (23rd September 2015)

-   Fix: only prepend -allTargets for -project builds (JENKINS-28256)
-   New: Add support for Xcode 7 date format in Unit Testing build task
    (pull request xcode-plugin/63)

#### Version 1.4.8 (28th February, 2015)

-   Fix: Add timeout to xcodebuild -list to avoid hang on xcode 6 (pull
    request xcode-plugin/58)
-   New: Support multiple targets to be passed into xcodebuild (pull
    request xcode-plugin/43)

#### Version 1.4.7 (5th January, 2015)

-   Fix: XCTest output parsing: support nested/namespaced test suites
    (JENKINS-26295)
-   New: XCTest output parsing: handle tests suites exiting with an
    error (pull request xcode-plugin/54)

#### Version 1.4.6 (16th December, 2014)

-   Fix: broken dSYM packaging (since 1.4.5)
-   Fix: BUILD\_DATE does not produce the correct last modified date if
    built on a slave machine (pull request xcode-plugin/50)
-   Fix: make sure to set provideApplicationVersion properly upon
    upgrade from per-1.4.1 (JENKINS-26027)
-   New: developerProfileLoader: use show-keychain-info to display job
    specific keychain information
-   New: support XC test output (JENKINS-19955)
-   New: display warning when simulator SDK selected and IPA about to be
    packaged (JENKINS-21293)
-   New: document xcodebuildArguments (JENKINS-13930)

#### Version 1.4.5 (10th December, 2014)

-   Fix: only zip the DSYM if the DSYM file was actually created and
    fail the build if the operation failed
-   Fix: Fail the build if we fail to create an ipa.
-   Fix: "keychain with the same name already exists" (JENKINS-22130)
-   New: XCode 6 compatibility (pull request xcode-plugin/48)
-   New: generate\_manifest added ability to generate enterprise
    distribution manifest plist (pull request xcode-plugin/45)
-   Fix: getKeychain returns a global keychain preferred over path (pull
    request xcode-plugin/41)
-   New: Ability to change the Bundle identifier (CFBundleIdentifier)
    for an xcode build (pull request xcode-plugin/39)

#### Version 1.4.2 (31st December, 2013)

-   Fix: Avoid NPE on plugin version update (JENKINS-19886, pull request
    xcode-plugin/37)

#### Version 1.4.1 (30th October, 2013)

-   Fix: performance regression in output parsing (JENKINS-20037, pull
    request xcode-plugin/36)

#### Version 1.4 (2nd Oct, 2013)

-   New: added an option to produce .xcarchive (JENKINS-14719)
-   Fix: Fixing show-keychain-info call (JENKINS-xcode-plugin/30)
-   New: Build wrapper to restore the keychain at the end of the build
    (pull request xcode-plugin/31)
-   New: Adding the possibility to provide a pattern for the .ipa file
    name (pull request xcode-plugin/33)
-   New: Added an ability to import \*.developerprofile into a build
    (pull request xcode-plugin/34)
-   New: UI update: group options into 3 categories (pull request
    xcode-plugin/28)
-   New: Introduced the ability to create global keychain configurations
-   New: Add new option "allow failing build results" (pull request
    xcode-plugin/25)
-   Fix: xcodebuild output parser is more lenient (pull request
    xcode-plugin/19)
-   Fix: Can't add xcodebuild parameters including whitespace
    (JENKINS-12800)
-   New: Add a field for entering code signing identity (pull request
    xcode-plugin/6)

#### Version 1.3.1 (27th March 2012)

-   Fix: Custom xcodebuild arguments values are not persisted
    (JENKINS-12510).

#### Version 1.3 (20th January 2012)

-   New : Ability to specify custom arguments to xcodebuild (so that
    values in project file can be overridden). It is needed to specify
    custom build options to make in-app unit tests work.
-   New : Ability to disable clean up of test reports on per-target
    level, so that it is possible to run several targets in single job
    and not mess-up test reports.
-   Fix : The plugin fails by searching for a double .app extension
    while compressing .dsym (JENKINS-12273)
-   Fix : The plugin fails to delete previous generated IPA results in a
    failed build (JENKINS-12237).
-   Fix : The plugin fails to set default keychain when using an
    alternate (non login) keychain (By default the login keychain is the
    default keychain) (JENKINS-12217).
-   Fix : Restore Java 1.5 runtime compatibility. (JENKINS-12378)

#### Version 1.2.2 (19th December 2011)

-   Fix : Build IPA fails if the plugin is launched on a remote agent
    using a relative path for its FS Root directory (JENKINS-12144)

#### Version 1.2.1 (18th December 2011)

-   Fix : Build IPA switch doesn't work properly for a default Xcode
    project always results in FATAL error (JENKINS-12089)

#### Version 1.2 (8th December 2011)

-   New build parameter to specify the build output directory. This
    overrides the setting in the user's project file, so it will be put
    into a known directory rather than XcodeDerivedData, etc. This makes
    it much easier to set up subsequent build steps in Jenkins that do
    something with the output of the build. It does this by passing the
    setting CONFIGURATION\_BUILD\_DIR to xcodebuild if a path is set for
    this new job config value.
-   Add support for building schemes and workspaces : The plugin now
    supports two extra configuration parameters `xcodeScheme` and
    `xcodeWorkspaceFile`. The scheme maps to xcodebuild's parameter
    "-scheme" and the workspace to "-workspace". The scheme takes
    precedence over the target setting and the workspace takes
    precedence over the project setting.

#### Version 1.1 (29th November 2011)

-   Upgrade the token macro plugin to version 1.5.1 to support
    environment and build variables (JENKINS-11892)
-   New configuration parameter to set the SYMROOT value passed to the
    build. This parameter accepts environment and build variables and
    Macros (JENKINS-11813)
-   Various code cleanup and improvements.

#### Version 1.0.1 (14th November 2011)

-   Minor fix about the default value and the documentation of the
    keystore path parameter.

#### Version 1.0 (14th November 2011)

-   Initiated from [Ray Yamamoto Hilton's Xcode plugin for
    Hudson](http://rayh.com.au/xcode-hudson-plugin) with few changes :
    -   It improves jenkins compatibility, and is available in its
        update center.
    -   It uses the to configure values of `CFBundleVersion` and
        `CFBundleShortVersionString`
    -   It allows to configure the keychain to use and to unlock it
    -   It is ready for internationalization

If you previously used the version provided by Ray you should be able to
test this one in parallel as it has a new identity. Also you'll have to
reconfigure a large part of it if you want to upgrade thus don't forget
to save your settings.

