package com.erigir.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;

/**
 * Created by chrweiss on 9/23/14.
 */
public class FileProcessorUtils {
    public static void copyFolder(File src, File dest)
            throws MojoExecutionException {

        try {
            if (src.isDirectory()) {

                //if directory not exists, create it
                if (!dest.exists()) {
                    dest.mkdir();
                    System.out.println("Directory copied from "
                            + src + "  to " + dest);
                }

                //list all the directory contents
                String files[] = src.list();

                for (String file : files) {
                    //construct the src and dest file structure
                    File srcFile = new File(src, file);
                    File destFile = new File(dest, file);
                    //recursive copy
                    copyFolder(srcFile, destFile);
                }

            } else {
                //if file, then copy it
                //Use bytes stream to support all file types
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];

                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            }
        }
        catch (IOException ioe)
        {
            throw new MojoExecutionException("Error cloning file/directory",ioe);
        }
    }
}
