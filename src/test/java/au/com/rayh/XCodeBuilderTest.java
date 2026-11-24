/*
 * The MIT License
 *
 * Copyright (c) 2012 Jerome Lacoste
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

package au.com.rayh;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.xcode.XcodeInstallation;
import hudson.plugins.xcode.XcodeXipInstaller;
import hudson.tools.InstallSourceProperty;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

public class XCodeBuilderTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() {}

    @After
    public void tearDown() {}

    @Test
    public void shouldSplitXcodeBuildArgumentsWithoutEscapedSpaces() throws Exception {
        assertEquals(
                Collections.singletonList("GCC_SYMBOLS_PRIVATE_EXTERN=NO"),
                XCodeBuilder.splitXcodeBuildArguments("GCC_SYMBOLS_PRIVATE_EXTERN=NO"));
        assertEquals(
                asList("GCC_SYMBOLS_PRIVATE_EXTERN=NO", "COPY_PHASE_STRIP=NO"),
                XCodeBuilder.splitXcodeBuildArguments("GCC_SYMBOLS_PRIVATE_EXTERN=NO COPY_PHASE_STRIP=NO"));
    }

    @Test
    public void shouldSplitXcodeBuildArgumentsWithEscapedSpaces() throws Exception {
        assertEquals(
                Collections.singletonList("CODE_SIGN_IDENTITY=iPhone Developer: Todd Kirby"),
                XCodeBuilder.splitXcodeBuildArguments("CODE_SIGN_IDENTITY=iPhone\\ Developer:\\ Todd\\ Kirby"));
        assertEquals(
                asList("A=B", "CODE_SIGN_IDENTITY=iPhone Developer: Todd Kirby"),
                XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY=iPhone\\ Developer:\\ Todd\\ Kirby"));
        assertEquals(
                asList("A=B", "CODE_SIGN_IDENTITY=iPhone Distribution", "C=D"),
                XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY=\"iPhone Distribution\" C=D"));
    }

    @Test
    public void shouldUnquoteWhenSplittingXcodeBuildArgumentsWithIncompleteDoubleQuote() throws Exception {
        assertEquals(
                asList("A=B", "CODE_SIGN_IDENTITY=iPhone Distribution"),
                XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY=\"iPhone Distribution"));
    }

    @Test
    public void shouldUnquoteWhenSplittingXcodeBuildArgumentsWithIncompleteSingleQuote() throws Exception {
        assertEquals(
                asList("A=B", "CODE_SIGN_IDENTITY=iPhone"),
                XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY='iPhone"));
    }

    @Test
    public void shouldInvokeAutoInstallerWhenResolvingXcodeDeveloperDir() throws Exception {
        File dummyDir = temp.newFolder();
        File dummyXip = new File(dummyDir, "Xcode_27.3_beta_2.xip");
        Files.writeString(dummyXip.toPath(), "mock xip", StandardCharsets.UTF_8);

        XcodeXipInstaller installer =
                new XcodeXipInstaller(null, dummyXip.toURI().toURL().toString(), "Xcode.app/Contents/Developer") {
                    @Override
                    protected boolean isXipAvailable(Node node) {
                        return true;
                    }

                    @Override
                    protected int extractXip(FilePath archive, FilePath destinationDir, Node node, TaskListener log)
                            throws IOException, InterruptedException {
                        destinationDir
                                .child("Xcode-beta.app/Contents/Developer")
                                .mkdirs();
                        return 0;
                    }
                };

        XcodeInstallation installation = new XcodeInstallation(
                "Xcode 27.3 beta 2",
                "",
                Collections.singletonList(new InstallSourceProperty(Collections.singletonList(installer))));
        XcodeInstallation.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(XcodeInstallation.DescriptorImpl.class);
        assertNotNull(descriptor);
        descriptor.setInstallations(installation);

        EnvVars envs = new EnvVars();
        Node node = XCodeBuilder.resolveExecutionNode(null, j.jenkins.getRootPath());
        assertNotNull(node);

        String resolved =
                XCodeBuilder.resolveAndApplyXcodeDeveloperDir("Xcode 27.3 beta 2", node, envs, TaskListener.NULL);

        assertNotNull(resolved);
        assertTrue(
                "Expected DEVELOPER_DIR to point to Xcode-beta.app/Contents/Developer, got: " + resolved,
                resolved.replace('\\', '/').endsWith("Xcode-beta.app/Contents/Developer"));
        assertEquals(resolved, envs.get("DEVELOPER_DIR"));
    }

    @Test
    public void shouldFailFastWhenConfiguredXcodeHomeResolvesEmpty() throws Exception {
        XcodeInstallation emptyInstallation = new XcodeInstallation("Xcode Empty", "", Collections.emptyList());
        XcodeInstallation.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(XcodeInstallation.DescriptorImpl.class);
        assertNotNull(descriptor);
        descriptor.setInstallations(emptyInstallation);

        EnvVars envs = new EnvVars();
        try {
            XCodeBuilder.resolveAndApplyXcodeDeveloperDir("Xcode Empty", j.jenkins, envs, TaskListener.NULL);
            fail("Expected AbortException when configured Xcode installation has empty home and no installer");
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("Xcode Empty"));
        }
        assertNull(envs.get("DEVELOPER_DIR"));
    }
}
