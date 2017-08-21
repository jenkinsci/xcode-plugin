package au.com.rayh;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class CodeSignOutputParserTests {
    CodeSignOutputParser parser;

    @Before
    public void setUp() {
        parser = new CodeSignOutputParser();
    }

    @After
    public void tearDown() {
    }

    public String read(String filename) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());
        byte[] encoded = Files.readAllBytes(Paths.get(file.getPath()));
        String content = new String(encoded, "UTF-8");

        return content;
    }

    @Test
    public void testValidoutput() throws IOException {
        String content = this.read("codesign-valid.txt");
        boolean res = this.parser.isValidOutput(content);
        assertEquals(true, res);
    }

    @Test
    public void testAnotherValidoutput() throws IOException {
        String content = this.read("codesign-valid2.txt");
        boolean res = this.parser.isValidOutput(content);
        assertEquals(true, res);
    }

    @Test
    public void testInvalidoutput() throws IOException {
        String content = this.read("codesign-invalid.txt");
        boolean res = this.parser.isValidOutput(content);
        assertEquals(false, res);
    }
}
