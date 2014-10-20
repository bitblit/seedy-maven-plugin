package com.erigir.maven.plugin;

public class ValidationSetting {

    private ValidationType type;
    private String includeRegex;

    public void setType(ValidationType type) {
        this.type = type;
    }

    public ValidationType getType() {
        return type;
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        if (includeRegex==null)
        {
            throw new IllegalArgumentException("Cannot set includeRegex to null");
        }
        this.includeRegex = includeRegex;
    }

    public static enum ValidationType
    {
        XML, JSON
    }
}
