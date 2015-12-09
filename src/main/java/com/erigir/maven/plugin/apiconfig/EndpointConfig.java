package com.erigir.maven.plugin.apiconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

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
