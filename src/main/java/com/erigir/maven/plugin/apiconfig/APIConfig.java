package com.erigir.maven.plugin.apiconfig;


import java.util.List;

/**
 * Created by cweiss1271 on 12/7/15.
 */
public class APIConfig {
    private String apiName;

    private List<EndpointConfig> endpoints;

    public List<EndpointConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }
}
