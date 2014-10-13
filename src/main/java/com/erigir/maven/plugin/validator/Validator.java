package com.erigir.maven.plugin.validator;

import com.erigir.maven.plugin.ValidatorSetting;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Created by maxkeene on 10/10/14.
 */
public interface Validator {
    public void validate(File src) throws MojoExecutionException;
}
