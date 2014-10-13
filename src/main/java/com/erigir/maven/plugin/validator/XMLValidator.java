package com.erigir.maven.plugin.validator;

import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Created by maxkeene on 10/10/14.
 */
public class XMLValidator implements Validator {
    @Override
    public void validate(File src) throws MojoExecutionException {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parser.parse(src);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new MojoExecutionException("found invalid xml in file: " + src.getName(), e);
        }
    }
}
