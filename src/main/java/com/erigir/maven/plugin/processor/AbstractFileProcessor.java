package com.erigir.maven.plugin.processor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;

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
public abstract class AbstractFileProcessor implements FileProcessor {

    public boolean process(Log log, File src)
            throws MojoExecutionException
    {
        log.info("Applying "+getClass().getSimpleName()+" to "+src.getName());

        try {
            if (!src.exists() || !src.isFile())
            {
                throw new MojoExecutionException(src+" doesnt exist or isnt a file");
            }

            File tmp = File.createTempFile("tst","tst");

            boolean rval = innerProcess(log, src,tmp);


            if (rval)
            {
                logDeltaIfExists(log, src, tmp);
                src.delete();
                tmp.renameTo(src);
            }
            else
            {
                tmp.delete();
            }

            return rval;
        } catch (IOException ioe)
        {
            throw new MojoExecutionException("Gzip failure on :"+src,ioe);
        }
    }

    public abstract boolean innerProcess(Log log, File src, File dst)
        throws MojoExecutionException, IOException;


    public void logDeltaIfExists(Log log, File src, File dst)
            throws IOException
    {
        long srcL = src.length();
        long dstL = dst.length();
        long delta = Math.abs(dstL-srcL);
        int pct = (int)(100.0*((double)delta/(double)srcL));
        if (srcL!=dstL)
        {
            log.info("Proc "+getClass().getSimpleName()+" mod file "+src.getName()+" from "+srcL+" to "+dstL+" ("+delta+" bytes "+pct+"%)");
        }
    }

}
