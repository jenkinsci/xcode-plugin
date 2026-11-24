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
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testDescriptorGetInstallationHandlesToolInstallationArray() {
        XcodeInstallation.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(XcodeInstallation.DescriptorImpl.class);
        assertNotNull(descriptor);

        ToolInstallation[] rawArray = new ToolInstallation[] {
            new XcodeInstallation("Xcode-16.3", "/Applications/Xcode.app/Contents/Developer", null)
        };
        // Simulate ToolDescriptor/JCasC setting a ToolInstallation[] runtime array
        ((hudson.tools.ToolDescriptor) descriptor).setInstallations(rawArray);

        XcodeInstallation[] typed = descriptor.getInstallations();
        assertEquals(1, typed.length);
        assertEquals("Xcode-16.3", typed[0].getName());
        assertNotNull(descriptor.getInstallation("Xcode-16.3"));
        assertEquals(1, XcodeInstallation.allInstallations().length);
    }

    @Test
    public void testBuildEnvVarsDisabledByDefault() {
        XcodeInstallation installation = new XcodeInstallation("Xcode 16", "/Applications/Xcode.app/Contents/Developer", null);
        assertFalse(installation.isExportDeveloperDir());

        EnvVars env = new EnvVars();
        installation.buildEnvVars(env);
        assertNull(env.get("DEVELOPER_DIR"));
    }

    @Test
    public void testBuildEnvVarsExportsDeveloperDirAcrossNodeAndEnvironment() throws Exception {
        XcodeInstallation installation = new XcodeInstallation("Xcode 16", "${BASE_XCODE}/Contents/Developer", null);
        installation.setExportDeveloperDir(true);
        assertTrue(installation.isExportDeveloperDir());

        XcodeInstallation forNode = installation.forNode(j.jenkins, TaskListener.NULL);
        assertTrue(forNode.isExportDeveloperDir());

        EnvVars baseEnv = new EnvVars("BASE_XCODE", "/opt/Xcode_16.3.app");
        XcodeInstallation forEnv = forNode.forEnvironment(baseEnv);
        assertTrue(forEnv.isExportDeveloperDir());

        EnvVars buildEnv = new EnvVars();
        forEnv.buildEnvVars(buildEnv);
        assertEquals("/opt/Xcode_16.3.app/Contents/Developer", buildEnv.get("DEVELOPER_DIR"));

        // Blank home should not inject empty DEVELOPER_DIR
        XcodeInstallation blankHome = new XcodeInstallation("Blank", "   ", null);
        blankHome.setExportDeveloperDir(true);
        EnvVars blankEnv = new EnvVars();
        blankHome.buildEnvVars(blankEnv);
        assertNull(blankEnv.get("DEVELOPER_DIR"));
    }
}
