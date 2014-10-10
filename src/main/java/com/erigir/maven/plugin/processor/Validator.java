package com.erigir.maven.plugin.processor;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

public interface Validator {

    /**
     * Validate the input {@link java.io.File}.
     * @param input the String to validate
     * @throws org.apache.maven.plugin.MojoExecutionException the input is invalid
     */
    void validate(File input) throws MojoExecutionException;
}
