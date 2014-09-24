package com.erigir.maven.plugin.processor;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * cweiss : 7/21/12 3:14 PM
 */
public class GZipFileProcessor extends AbstractFileProcessor{

    public static int totalSaved = 0;

    public boolean innerProcess(Log log, File src, File dst)
            throws MojoExecutionException, IOException
    {

        InputStream is = new FileInputStream(src);
        OutputStream os = new GZIPOutputStream(new FileOutputStream(dst));

        IOUtils.copy(is,os);

        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);

        long delta = dst.length()-src.length();
        totalSaved += delta;
        return true;
    }

}
