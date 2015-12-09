package com.erigir.maven.plugin.s3uploadparam;

import com.erigir.wrench.drigo.ProcessReplace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Copyright 2014-2015 Christopher Weiss
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
public class ProcessReplaceParam {
    private String includeRegex;
    private String prefix;
    private String suffix;
    private LinkedHashMap<String, String> replace = new LinkedHashMap<>();

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

    public LinkedHashMap<String, String> getReplace() {
        return replace;
    }

    public void setReplace(LinkedHashMap<String, String> replace) {
        this.replace = replace;
    }

    public ProcessReplace toDrigo() {
        ProcessReplace rval = new ProcessReplace();
        rval.setIncludeRegex(includeRegex);
        rval.setPrefix(prefix);
        rval.setSuffix(suffix);

        LinkedHashMap<Pattern, String> r = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : replace.entrySet()) {
            r.put(Pattern.compile(e.getKey()), e.getValue());
        }

        rval.setReplace(r);
        return rval;
    }
}
