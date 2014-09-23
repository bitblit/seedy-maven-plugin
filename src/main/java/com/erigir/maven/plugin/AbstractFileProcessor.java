package com.erigir.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * cweiss : 7/21/12 3:14 PM
 */
public abstract class AbstractFileProcessor implements FileProcessor {

    public boolean process(Log log, File src)
            throws MojoExecutionException
    {
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
