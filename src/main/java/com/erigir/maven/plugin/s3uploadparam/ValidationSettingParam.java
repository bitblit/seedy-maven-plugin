package com.erigir.maven.plugin.s3uploadparam;

import com.erigir.wrench.drigo.ValidationSetting;
import com.erigir.wrench.drigo.ValidationSetting.ValidationType;

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

public class ValidationSettingParam {

    private ValidationType type;
    private String includeRegex;

    public ValidationType getType() {
        return type;
    }

    public void setType(ValidationType type) {
        this.type = type;
    }

    public ValidationSetting toDrigo() {
        ValidationSetting rval = new ValidationSetting();
        rval.setIncludeRegex(includeRegex);
        rval.setType(type);

        return rval;
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        if (includeRegex == null) {
            throw new IllegalArgumentException("Cannot set includeRegex to null");
        }
        this.includeRegex = includeRegex;
    }

}
