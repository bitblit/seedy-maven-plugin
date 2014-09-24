package com.erigir.maven.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.util.List;

/**
 * Finds all resources that match the include regex, and combines them into a single file,
 * outputfilename
 *
 * Then, finds any files matching replaceInHtmlRegex, and finds and comments in them of the form:
 * <!--{flagName}--></!--END-{flagName}-->
 * and replaces them with replaceText
 *
 * Created by chrweiss on 9/23/14.
 */
public class HtmlResourceBatching {
    private String flagName;
    private ReplaceTextWrapper wrapper = ReplaceTextWrapper.NONE;
    private String includeRegex;
    private String replaceInHtmlRegex;
    private String outputFileName;
    private String replaceText;
    private boolean deleteSource=false;
    private String fileSeparator="\n";

    public String getWrappedReplaceText() {return wrapper.wrap(replaceText);}

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

    public static enum ReplaceTextWrapper
    {
        NONE("",""),
        JAVASCRIPT("<script src=\"","\"></script>"),
        CSS("<link rel=\"stylesheet\" href=\"","\" />");

        String pre;
        String post;

        ReplaceTextWrapper(String pre,String post)
        {
            this.post = post;
            this.pre = pre;
        }

        public String wrap(String value)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(pre);
            sb.append((value==null)?"":value);
            sb.append(post);
            return sb.toString();
        }

    }


    public void combine(List<File> src,File output)
            throws MojoExecutionException
    {
        try {
            OutputStream os = new FileOutputStream(output);
            for (int i=0;i<src.size();i++)
            {
                File f = src.get(i);
                if (i>0 && fileSeparator!=null)
                {
                    os.write(fileSeparator.getBytes());
                }
                IOUtils.copy(new FileInputStream(f),os);
            }

            IOUtils.closeQuietly(os);
        } catch (IOException ioe)
        {
            throw new MojoExecutionException("Error combining",ioe);

        }
    }


}
