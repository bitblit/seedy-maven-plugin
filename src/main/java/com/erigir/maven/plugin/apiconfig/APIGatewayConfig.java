package com.erigir.maven.plugin.apiconfig;

import java.util.List;

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
