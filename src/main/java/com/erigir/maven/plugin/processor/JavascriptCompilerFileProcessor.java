package com.erigir.maven.plugin.processor;

import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by chrweiss on 9/23/14.
 */
public class JavascriptCompilerFileProcessor extends AbstractFileProcessor {

    private RuntimeExecClosureCompiler runtimeExecClosureCompiler = new RuntimeExecClosureCompiler();

    @Override
    public boolean innerProcess(Log log, File src, File dst) throws MojoExecutionException, IOException {
        String js = IOUtils.toString(new FileInputStream(src));
        String out = InProcessClosureCompiler.compileJavascriptString(log, js);
        IOUtils.write(out, new FileOutputStream(dst));
        return true;
    }

    public boolean innerProcessNew(Log log, File src, File dst) throws MojoExecutionException, IOException {
        //String js = IOUtils.toString(new FileInputStream(src));
        String out = runtimeExecClosureCompiler.compile(CompilationLevel.SIMPLE_OPTIMIZATIONS, Arrays.asList(src));
        //InProcessClosureCompiler.compileJavascriptString(log, js);

        IOUtils.write(out, new FileOutputStream(dst));
        return true;
    }


}
