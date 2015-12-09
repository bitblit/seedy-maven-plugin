package com.erigir.maven.plugin.apiconfig;

import java.util.List;

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

/**
 * Created by cweiss1271 on 12/7/15.
 */
public class APIGatewayConfig {
    private boolean enableCORS;
    private String resourcePath;
    private List<String> resourceMethods;

    public boolean isEnableCORS() {
        return enableCORS;
    }

    public void setEnableCORS(boolean enableCORS) {
        this.enableCORS = enableCORS;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public List<String> getResourceMethods() {
        return resourceMethods;
    }

    public void setResourceMethods(List<String> resourceMethods) {
        this.resourceMethods = resourceMethods;
    }

    public String parentPath() {
        int idx = resourcePath.lastIndexOf("/");
        String parentPath = resourcePath.substring(0, idx);
        return parentPath;
    }

    public String endPathPart() {
        int idx = resourcePath.lastIndexOf("/");
        return resourcePath.substring(idx + 1);
    }
}
