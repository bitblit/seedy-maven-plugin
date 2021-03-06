package com.erigir.maven.plugin.s3uploadparam;

import com.erigir.wrench.drigo.HtmlResourceBatching;
import com.erigir.wrench.drigo.HtmlResourceBatching.ReplaceTextWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.util.List;

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
 * Finds all resources that match the include regex, and combines them into a single file,
 * outputfilename
 * <p>
 * Then, finds any files matching replaceInHtmlRegex, and finds and comments in them of the form:
 * &lt;!--{flagName}--&gt;&lt;/!--END-{flagName}--&gt;
 * and replaces them with replaceText
 * <p>
 * Created by chrweiss on 9/23/14.
 */
public class HtmlResourceBatchingParam {
    private String flagName;
    private ReplaceTextWrapper wrapper = ReplaceTextWrapper.NONE;
    private String includeRegex;
    private String replaceInHtmlRegex;
    private String outputFileName;
    private String replaceText;
    private boolean deleteSource = false;
    private String fileSeparator = "\n";

    public HtmlResourceBatching toDrigoBatching() {
        HtmlResourceBatching rval = new HtmlResourceBatching();
        rval.setFlagName(flagName);
        rval.setIncludeRegex(includeRegex);
        rval.setReplaceInHtmlRegex(replaceInHtmlRegex);
        rval.setOutputFileName(outputFileName);
        rval.setReplaceText(replaceText);
        rval.setDeleteSource(deleteSource);
        rval.setFileSeparator(fileSeparator);
        return rval;
    }

    public String getWrappedReplaceText() {
        return wrapper.wrap(replaceText);
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public String getFlagName() {
        return flagName;
    }

    public void setFlagName(String flagName) {
        this.flagName = flagName;
    }

    public String getReplaceInHtmlRegex() {
        return replaceInHtmlRegex;
    }

    public void setReplaceInHtmlRegex(String replaceInHtmlRegex) {
        this.replaceInHtmlRegex = replaceInHtmlRegex;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public String getReplaceText() {
        return replaceText;
    }

    public void setReplaceText(String replaceText) {
        this.replaceText = replaceText;
    }

    public boolean isDeleteSource() {
        return deleteSource;
    }

    public void setDeleteSource(boolean deleteSource) {
        this.deleteSource = deleteSource;
    }

    public String getFileSeparator() {
        return fileSeparator;
    }

    public void setFileSeparator(String fileSeparator) {
        this.fileSeparator = fileSeparator;
    }

    public void combine(List<File> src, File output)
            throws MojoExecutionException {
        try {
            OutputStream os = new FileOutputStream(output);
            for (int i = 0; i < src.size(); i++) {
                File f = src.get(i);
                if (i > 0 && fileSeparator != null) {
                    os.write(fileSeparator.getBytes());
                }
                IOUtils.copy(new FileInputStream(f), os);
            }

            IOUtils.closeQuietly(os);
        } catch (IOException ioe) {
            throw new MojoExecutionException("Error combining", ioe);

        }
    }


}
