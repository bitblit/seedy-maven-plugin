package com.erigir.maven.plugin;

/**
 * Created by summedew on 10/9/14.
 */
public class Validator {

    private String includeRegex;
    private ValidatorType type = ValidatorType.JSON;

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public ValidatorType getType() {
        return type;
    }

    public void setType(ValidatorType type) {
        this.type = type;
    }

    public static enum ValidatorType {
        JSON
    }
}
