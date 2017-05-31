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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;
import static java.util.Arrays.asList;


public class XCodeBuilderTest {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void shouldSplitXcodeBuildArgumentsWithoutEscapedSpaces() throws Exception {
        assertEquals(Collections.singletonList("GCC_SYMBOLS_PRIVATE_EXTERN=NO"),
            XCodeBuilder.splitXcodeBuildArguments("GCC_SYMBOLS_PRIVATE_EXTERN=NO"));
        assertEquals(asList("GCC_SYMBOLS_PRIVATE_EXTERN=NO", "COPY_PHASE_STRIP=NO"),
            XCodeBuilder.splitXcodeBuildArguments("GCC_SYMBOLS_PRIVATE_EXTERN=NO COPY_PHASE_STRIP=NO"));
    }

    @Test
    public void shouldSplitXcodeBuildArgumentsWithEscapedSpaces() throws Exception {
        assertEquals(Collections.singletonList("CODE_SIGN_IDENTITY=iPhone Developer: Todd Kirby"),
            XCodeBuilder.splitXcodeBuildArguments("CODE_SIGN_IDENTITY=iPhone\\ Developer:\\ Todd\\ Kirby"));
        assertEquals(asList("A=B", "CODE_SIGN_IDENTITY=iPhone Developer: Todd Kirby"),
            XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY=iPhone\\ Developer:\\ Todd\\ Kirby"));
        assertEquals(asList("A=B", "CODE_SIGN_IDENTITY=iPhone Distribution", "C=D"),
            XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY=\"iPhone Distribution\" C=D"));
    }
    @Test
    public void shouldUnquoteWhenSplittingXcodeBuildArgumentsWithIncompleteDoubleQuote() throws Exception {
        assertEquals(asList("A=B", "CODE_SIGN_IDENTITY=iPhone Distribution"),
            XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY=\"iPhone Distribution"));
    }
    @Test
    public void shouldUnquoteWhenSplittingXcodeBuildArgumentsWithIncompleteSingleQuote() throws Exception {
        assertEquals(asList("A=B", "CODE_SIGN_IDENTITY=iPhone"),
            XCodeBuilder.splitXcodeBuildArguments("A=B CODE_SIGN_IDENTITY='iPhone"));
    }
}