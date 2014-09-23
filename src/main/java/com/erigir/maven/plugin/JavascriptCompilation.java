package com.erigir.maven.plugin;

/**
 * Created by chrweiss on 9/23/14.
 */
public class JavascriptCompilation {
    private JSCompilationMode mode = JSCompilationMode.CLOSURE_BASIC;
    private boolean combine = false;
    private String includeRegex;

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public JSCompilationMode getMode() {
        return mode;
    }

    public void setMode(JSCompilationMode mode) {
        this.mode = mode;
    }

    public boolean isCombine() {
        return combine;
    }

    public void setCombine(boolean combine) {
        this.combine = combine;
    }

    public static enum JSCompilationMode
    {
        CLOSURE_WHITESPACE, CLOSURE_BASIC, CLOSURE_ADVANCED;
    }

}
