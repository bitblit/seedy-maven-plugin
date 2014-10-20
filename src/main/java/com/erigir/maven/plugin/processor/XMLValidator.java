package com.erigir.maven.plugin.processor;

import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 Copyright 2014 Christopher Weiss

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
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
