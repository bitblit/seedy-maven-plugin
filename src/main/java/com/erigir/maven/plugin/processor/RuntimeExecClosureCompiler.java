package com.erigir.maven.plugin.processor;

import com.google.javascript.jscomp.CompilationLevel;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A pretty dumb wrapper around closure - runs in a forked JVM so we don't have to worry about
 * closure's System.exit killing our system.
 *
 * cweiss : 7/18/12 11:26 AM
 */
public class RuntimeExecClosureCompiler {

    public String compile(CompilationLevel level, List<File> inputFiles)
            throws IOException
    {
        try {
            Runtime runtime = Runtime.getRuntime();

            List<String> cmd = new LinkedList<>();
            cmd.add("java");
            cmd.add("-cp");
            cmd.add(buildClasspathArgument());
            cmd.add("com.google.javascript.jscomp.CommandLineRunner");

            for (File f : inputFiles) {
                cmd.add("--js");
                cmd.add(f.getAbsolutePath());
            }

            cmd.add("--compilation_level");
            cmd.add(level.toString());


            Process p = runtime.exec(cmd.toArray(new String[0]));

            p.waitFor();

            if (p.exitValue()==0)
            {
                return IOUtils.toString(p.getInputStream());
            }
            else
            {
                throw new IllegalStateException("Error occurred while processing : "+IOUtils.toString(p.getErrorStream()));
            }
        }
        catch (InterruptedException ie)
        {
            throw new RuntimeException("Interrupted while running",ie);
        }

    }

    public String buildClasspathArgument()
    {
        // Get path to closure compiler
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl==null)
        {
            cl = ClassLoader.getSystemClassLoader();
        }

        List<URL> allUrls = new LinkedList<>();

        while (cl!=null)
        {
            if (URLClassLoader.class.isAssignableFrom(cl.getClass()))
            {
                URLClassLoader u = (URLClassLoader)cl;
                allUrls.addAll(Arrays.asList(u.getURLs()));
            }
            cl = cl.getParent();
        }

        StringBuilder sb = new StringBuilder();

        for (int i=0;i<allUrls.size();i++)
        {
            if (i>0)
            {
                sb.append(File.pathSeparator);
            }
            sb.append(allUrls.get(i));
        }
        return sb.toString();
    }



}
