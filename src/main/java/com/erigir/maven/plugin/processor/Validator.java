package com.erigir.maven.plugin.processor;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

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

public interface Validator {

    /**
     * Validate the input {@link java.io.File}.
     * @param input the String to validate
     * @throws org.apache.maven.plugin.MojoExecutionException the input is invalid
     */
    void validate(File input) throws MojoExecutionException;
}
