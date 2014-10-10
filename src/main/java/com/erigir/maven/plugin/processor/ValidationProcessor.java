package com.erigir.maven.plugin.processor;

import com.erigir.maven.plugin.ValidationSetting;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ValidationProcessor extends AbstractFileProcessor {

    private final Validator validator;

    public ValidationProcessor(ValidationSetting.ValidationType type) {

        switch (type) {
            case JSON:
                validator = new JSONValidator();
                break;
            case XML:
                validator = new XMLValidator();
                break;
            default:
                throw new IllegalArgumentException("Cannot set validator type to: " + type);
        }
    }

    @Override
    public boolean innerProcess(Log log, File src, File dst) throws MojoExecutionException, IOException {
        validator.validate(src);

        String input = IOUtils.toString(new FileInputStream(src));
        IOUtils.write(input, new FileOutputStream(dst));
        return true;
    }
}
