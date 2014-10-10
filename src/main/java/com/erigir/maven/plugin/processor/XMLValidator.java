package com.erigir.maven.plugin.processor;

import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Ensure that a {@link File} can be parsed into XML.
 */
public class XMLValidator implements Validator {

    @Override
    public void validate(File input) throws MojoExecutionException {

        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parser.parse(input);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new MojoExecutionException("XML Validation failed for:" + input, e);
        }
    }
}
