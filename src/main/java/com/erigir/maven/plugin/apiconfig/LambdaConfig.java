package com.erigir.maven.plugin.apiconfig;

import com.amazonaws.services.lambda.model.Runtime;

/**
 * Created by cweiss1271 on 12/7/15.
 */
public class LambdaConfig {
    private String functionName;
    private Runtime runtime;
    private int timeoutInSeconds;
    private int memoryInMb;
    private String roleArn;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    public int getTimeoutInSeconds() {
        return timeoutInSeconds;
    }

    public void setTimeoutInSeconds(int timeoutInSeconds) {
        this.timeoutInSeconds = timeoutInSeconds;
    }

    public int getMemoryInMb() {
        return memoryInMb;
    }

    public void setMemoryInMb(int memoryInMb) {
        this.memoryInMb = memoryInMb;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }
}
