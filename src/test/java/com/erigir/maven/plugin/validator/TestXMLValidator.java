package com.erigir.maven.plugin.validator;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by maxkeene on 10/13/14.
 */
public class TestXMLValidator {

    @Test
    public void testValidXML() throws IOException, MojoExecutionException {
        XMLValidator xmlValidator = new XMLValidator();
        String jsonString =
                "<tag1>\n" +
                "    <tag2 attr=\"value\">\n" +
                "        <tag3 attr2=\"value2\">\n" +
                "            <tag4 />\n" +
                "        </tag3>\n" +
                "    </tag2>\n" +
                "</tag1>";
        File xmlFile = new File("test.json");
        FileOutputStream fos = new FileOutputStream(xmlFile);
        fos.write(jsonString.getBytes());
        fos.close();

        xmlValidator.validate(xmlFile);

        xmlFile.delete();
    }

    @Test(expected = MojoExecutionException.class)
    public void testInvalidXML() throws IOException, MojoExecutionException {
        XMLValidator xmlValidator = new XMLValidator();
        String jsonString =
                "<tag1>\n" +
                "    <tag2 attr=value\">\n" +
                "        <tag3 attr2=\"value2\">\n" +
                "            <tag4 />\n" +
                "        </tag3>\n" +
                "    </tag2>\n" +
                "</tag1>";
        File xmlFile = new File("test.xml");
        FileOutputStream fos = new FileOutputStream(xmlFile);
        fos.write(jsonString.getBytes());
        fos.close();

        xmlValidator.validate(xmlFile);

        xmlFile.delete();
    }
}
