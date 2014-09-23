package com.erigir.maven.plugin;

/**
 * Created by chrweiss on 9/23/14.
 */
public class CssCompilation {
    private boolean combine = false;

    private String includeRegex;

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public boolean isCombine() {
        return combine;
    }

    public void setCombine(boolean combine) {
        this.combine = combine;
    }

}
