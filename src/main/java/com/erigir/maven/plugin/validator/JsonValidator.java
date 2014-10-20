package com.erigir.maven.plugin.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by maxkeene on 10/10/14.
 */
public class JSONValidator implements Validator {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void validate(File src) throws MojoExecutionException {
        try {
            mapper.readValue(src, Map.class);
        } catch (IOException e) {
            throw new MojoExecutionException("found invalid json in file " + src.getName(), e);
        }
    }
}
