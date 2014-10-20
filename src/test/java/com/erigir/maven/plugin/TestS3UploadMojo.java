package com.erigir.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by chrweiss on 9/18/14.
 */
public class TestS3UploadMojo {
    private static String BUCKET_NAME="chirp.allpointpen.com";

    @Test
    @Ignore
    public void testInclusion()
            throws MojoExecutionException
    {
        S3UploadMojo s = new S3UploadMojo();
        //s.doNotUpload=true;
        s.s3Bucket=BUCKET_NAME;
        s.source="src/test";
        s.recursive=true;

        FileCompression fc = new FileCompression();
        fc.setIncludeRegex(".*\\.java");

        s.fileCompression = fc;

        ObjectMetadataSetting uc = new ObjectMetadataSetting();
        uc.setIncludeRegex(".*\\.java");
        uc.setCacheControl("Max-Age = 30");
        uc.getUserMetaData().put("mykey","myval");
        uc.setContentType("text/java");

        s.objectMetadataSettings = Arrays.asList(uc);

        s.execute();

    }
}
