package com.erigir.maven.plugin.processor;

import com.erigir.maven.plugin.HtmlResourceBatching;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * cweiss : 7/21/12 3:14 PM
 */
public class ApplyFilterProcessor extends AbstractFileProcessor{

    private HtmlResourceBatching batching;

    public ApplyFilterProcessor(HtmlResourceBatching batching) {
        this.batching = batching;
    }

    public boolean innerProcess(Log log, File src, File dst)
            throws MojoExecutionException, IOException
    {
        String contents = IOUtils.toString(new FileInputStream(src));

        String startTag = "<!--"+batching.getFlagName()+"-->";
        String endTag = "<!--END:"+batching.getFlagName()+"-->";

        log.info("Searching "+src.getName()+" for "+startTag+" to "+endTag);
        int startIdx = contents.indexOf(startTag);

        int count = 0;
        while (startIdx!=-1)
        {
            int endIdx = contents.indexOf(endTag);
            if (endIdx==-1)
            {
                throw new MojoExecutionException("Couldn't find end tag : "+endTag);
            }
            count++;
            contents = contents.substring(0,startIdx)+batching.getWrappedReplaceText()+contents.substring(endIdx+(endTag.length()));
            startIdx = contents.indexOf(startTag);
        }

        FileOutputStream os = new FileOutputStream(dst);
        IOUtils.write(contents, os);
        IOUtils.closeQuietly(os);

        log.info("Found "+count+" instances");

        return true;
    }

}
