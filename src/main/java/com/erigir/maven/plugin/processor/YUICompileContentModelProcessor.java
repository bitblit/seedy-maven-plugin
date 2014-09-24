package com.erigir.maven.plugin.processor;

import com.yahoo.platform.yui.compressor.CssCompressor;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;

/**
 * cweiss : 7/21/12 3:14 PM
 */
public class YUICompileContentModelProcessor extends AbstractFileProcessor{

    @Override
    public boolean innerProcess(Log log, File src, File dst)
    throws MojoExecutionException, IOException
    {
        // Run the yahoo compiler
        CssCompressor css = new CssCompressor(new FileReader(src));
        FileWriter out = new FileWriter(dst);
        css.compress(out, 0);
        IOUtils.closeQuietly(out);
        return true;
    }

}
