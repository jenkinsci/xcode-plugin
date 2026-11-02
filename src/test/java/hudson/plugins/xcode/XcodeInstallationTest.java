/*
 * The MIT License
 *
 * Copyright (c) 2026 Jenkins Xcode Plugin Contributors
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

package hudson.plugins.xcode;

import hudson.EnvVars;
import hudson.util.FormValidation;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class XcodeInstallationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testDescriptorDoCheckHome() {
        XcodeInstallation.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(XcodeInstallation.DescriptorImpl.class);
        assertNotNull(descriptor);

        // Null value returns OK
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckHome(null).kind);

        // Empty path returns OK
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckHome(new File("")).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckHome(new File("   ")).kind);

        // Nonexistent path returns validation failure
        FormValidation validation = descriptor.doCheckHome(new File("/nonexistent/developer/path"));
        assertNotEquals(FormValidation.Kind.OK, validation.kind);
    }

    @Test
    public void testXcodeInstallationProperties() {
        XcodeInstallation installation = new XcodeInstallation("Xcode 15", "/Applications/Xcode.app/Contents/Developer", null);
        assertEquals("Xcode 15", installation.getName());
        assertEquals("/Applications/Xcode.app/Contents/Developer", installation.getHome());
        assertEquals("/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild", installation.getXcodebuild());
        assertNotNull(installation.getDescriptor());

        EnvVars env = new EnvVars();
        env.put("XCODE_HOME", "/custom/Xcode.app/Contents/Developer");
        XcodeInstallation withEnv = new XcodeInstallation("Xcode Env", "${XCODE_HOME}", null);
        XcodeInstallation expanded = withEnv.forEnvironment(env);
        assertEquals("/custom/Xcode.app/Contents/Developer", expanded.getHome());
    }
}
