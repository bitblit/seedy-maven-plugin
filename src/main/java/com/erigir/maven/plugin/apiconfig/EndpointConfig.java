package com.erigir.maven.plugin.apiconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by cweiss1271 on 12/7/15.
 */
public class EndpointConfig {
    private String description;
    private String className;

    @JsonProperty("lambda")
    private LambdaConfig lambdaConfig;
    @JsonProperty("apiGateway")
    private APIGatewayConfig apiGatewayConfig;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public LambdaConfig getLambdaConfig() {
        return lambdaConfig;
    }

    public void setLambdaConfig(LambdaConfig lambdaConfig) {
        this.lambdaConfig = lambdaConfig;
    }

    public APIGatewayConfig getApiGatewayConfig() {
        return apiGatewayConfig;
    }

    public void setApiGatewayConfig(APIGatewayConfig apiGatewayConfig) {
        this.apiGatewayConfig = apiGatewayConfig;
    }
}
