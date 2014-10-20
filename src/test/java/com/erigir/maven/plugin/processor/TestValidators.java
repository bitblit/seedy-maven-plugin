package com.erigir.maven.plugin.processor;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.io.File;

public class TestValidators {

    @Test
    public void testValidJSON()
            throws Exception
    {
        File goodJSON = new File(getClass().getResource("/validate/good.json").getFile());

        JSONValidator jsonValidator = new JSONValidator();
        jsonValidator.validate(goodJSON);
    }

    @Test(expected = MojoExecutionException.class)
    public void testInValidJSON()
            throws Exception {
        File badJSON = new File(getClass().getResource("/validate/bad.json").getFile());

        JSONValidator jsonValidator = new JSONValidator();
        jsonValidator.validate(badJSON);
    }

    @Test
    public void testValidXML()
            throws Exception
    {
        File goodXML = new File(getClass().getResource("/validate/good.xml").getFile());

        XMLValidator xmlValidator = new XMLValidator();
        xmlValidator.validate(goodXML);
    }

    @Test(expected = MojoExecutionException.class)
    public void testInValidXML()
            throws Exception {
        File badXML = new File(getClass().getResource("/validate/bad.xml").getFile());

        XMLValidator xmlValidator = new XMLValidator();
        xmlValidator.validate(badXML);
    }
}
