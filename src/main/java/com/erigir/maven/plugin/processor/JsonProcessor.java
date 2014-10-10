package com.erigir.maven.plugin.processor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by summedew on 10/9/14.
 */
public class JsonProcessor extends AbstractFileProcessor {
    @Override
    public boolean innerProcess(Log log, File src, File dst) throws MojoExecutionException, IOException {

        String json = IOUtils.toString(new FileInputStream(src));
        try {
            final JsonParser parser = new ObjectMapper().getFactory().createParser(json);
            while (parser.nextToken() != null) {
            }
        } catch (JsonParseException jpe) {
            throw new MojoExecutionException("Unable to validate json.", jpe);
        }

        return true;
    }
}
