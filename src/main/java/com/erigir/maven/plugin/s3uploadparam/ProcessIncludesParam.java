package com.erigir.maven.plugin.s3uploadparam;

import com.erigir.wrench.drigo.ProcessIncludes;

/**
 * Copyright 2014 Christopher Weiss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
public class ProcessIncludesParam {
    private String includeRegex;
    private String prefix;
    private String suffix;

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public ProcessIncludes toDrigo()
    {
        ProcessIncludes rval = new ProcessIncludes();
        rval.setIncludeRegex(includeRegex);
        rval.setPrefix(prefix);
        rval.setSuffix(suffix);
        return rval;
    }
}
