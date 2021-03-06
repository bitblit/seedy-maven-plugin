package com.erigir.maven.plugin.s3uploadparam;

import com.erigir.wrench.drigo.JavascriptCompilation;
import com.erigir.wrench.drigo.JavascriptCompilation.JSCompilationMode;

/*
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
public class JavascriptCompilationParam {
    private JSCompilationMode mode = JSCompilationMode.CLOSURE_BASIC;
    private String includeRegex;

    public JavascriptCompilation toDrigo() {
        JavascriptCompilation rval = new JavascriptCompilation();
        rval.setIncludeRegex(includeRegex);
        rval.setMode(mode);
        return rval;
    }


    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public JSCompilationMode getMode() {
        return mode;
    }

    public void setMode(JSCompilationMode mode) {
        this.mode = mode;
    }


}
