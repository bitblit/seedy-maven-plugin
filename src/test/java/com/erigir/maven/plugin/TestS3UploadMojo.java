package com.erigir.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by chrweiss on 9/18/14.
 */
public class TestS3UploadMojo {

    @Test
    @Ignore
    public void testInclusion()
            throws MojoExecutionException
    {
        S3UploadMojo s = new S3UploadMojo();
        //s.doNotUpload=true;
        s.s3Bucket="chirp.allpointpen.com";
        s.source="src/test";
        s.recursive=true;

        UploadConfig uc = new UploadConfig();
        uc.setIncludeRegex(".*\\.java");
        uc.setCompress(true);
        uc.setCacheControl("Max-Age = 30");
        uc.getUserMetaData().put("mykey","myval");
        uc.setContentType("text/java");

        s.uploadConfigs = Arrays.asList(uc);

        s.execute();

    }
}
