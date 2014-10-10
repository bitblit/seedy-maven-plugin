package com.erigir.maven.plugin.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

/**
 * Ensure that a {@link File} can be parsed into JSON.
 */
public class JSONValidator implements Validator {

    @Override
    public void validate(File input) throws MojoExecutionException {
        try {
            final JsonParser parser = new ObjectMapper().getFactory().createParser(input);

            while (parser.nextToken() != null) {
            }

        } catch (IOException e) {
            throw new MojoExecutionException("JSON validation failed for: " + input , e);
        }
    }
}
