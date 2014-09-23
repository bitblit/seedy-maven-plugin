package com.erigir.maven.plugin;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by chrweiss on 9/23/14.
 */
public interface FileProcessor {

    // All files processed IN-PLACE
    boolean process(Log log, File src)
            throws MojoExecutionException;
}
