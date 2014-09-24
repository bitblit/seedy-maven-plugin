package com.erigir.maven.plugin.processor;

import com.google.javascript.jscomp.CompilationLevel;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chrweiss on 9/23/14.
 */
public class TestRuntimeExecClosureCompiler {

    @Test
    public void testCompiler()
            throws Exception
    {
        File t1 = new File(getClass().getResource("/js/test1.js").getFile());
        File t2 = new File(getClass().getResource("/js/test2.js").getFile());
        List<File> files = Arrays.asList(t1, t2);

        RuntimeExecClosureCompiler cc = new RuntimeExecClosureCompiler();
        String output = cc.compile(CompilationLevel.SIMPLE_OPTIMIZATIONS, files);

        System.out.println("out: "+output);
    }
}
