package com.erigir.maven.plugin.processor;

import com.erigir.maven.plugin.HtmlResourceBatching;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 Copyright 2014 Christopher Weiss

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
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
