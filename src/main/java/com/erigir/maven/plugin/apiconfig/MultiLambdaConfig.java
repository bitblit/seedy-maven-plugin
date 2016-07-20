package com.erigir.maven.plugin.apiconfig;

import java.util.List;

/**
 * Created by cweiss1271 on 2/8/16.
 */
public class MultiLambdaConfig {
    private String functionNamePrefix;
    private String functionNameSuffix;
    private List<LambdaConfig> functions;

    public String getFunctionNamePrefix() {
        return functionNamePrefix;
    }

    public void setFunctionNamePrefix(String functionNamePrefix) {
        this.functionNamePrefix = functionNamePrefix;
    }

    public String getFunctionNameSuffix() {
        return functionNameSuffix;
    }

    public void setFunctionNameSuffix(String functionNameSuffix) {
        this.functionNameSuffix = functionNameSuffix;
    }

    public List<LambdaConfig> getFunctions() {
        return functions;
    }

    public void setFunctions(List<LambdaConfig> functions) {
        this.functions = functions;
    }

    public String fullName(LambdaConfig config)
    {
        StringBuilder sb = new StringBuilder();
        if (functionNamePrefix!=null)
        {
            sb.append(functionNamePrefix.trim());
        }
        sb.append(config.getFunctionName());
        if (functionNameSuffix!=null)
        {
            sb.append(functionNameSuffix.trim());
        }
        return sb.toString();

    }

}
