package com.erigir.maven.plugin;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by chrweiss on 9/23/14.
 */
public class JavascriptCompilerFileProcessor extends AbstractFileProcessor {

    @Override
    public boolean innerProcess(Log log, File src, File dst) throws MojoExecutionException, IOException {

        String js = IOUtils.toString(new FileInputStream(src));
        String out = InProcessClosureCompiler.compileJavascriptString(log, js);
        IOUtils.write(out, new FileOutputStream(dst));
        return true;
    }



}
