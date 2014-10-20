package com.erigir.maven.plugin.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;

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
