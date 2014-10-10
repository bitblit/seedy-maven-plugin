package com.erigir.maven.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by chrweiss on 9/18/14.
 */
public class TestS3UploadMojo {

    private final String bucket = "chirp.allpointpen.com";
    @Test
    //@Ignore
    public void testInclusion()
            throws MojoExecutionException
    {
        S3UploadMojo s = new S3UploadMojo();
        //s.doNotUpload=true;
        s.s3Bucket= bucket;
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

    @Test(expected = MojoExecutionException.class)
    public void testInvalidJsonValidation() throws MojoExecutionException, IOException {

        S3UploadMojo s = new S3UploadMojo();
        String json = "{ glossary: title {\"example\"}}";

        File myTemp = new File("src/test", UUID.randomUUID().toString() + ".json");
        myTemp.deleteOnExit(); // clean up after ourselves
        FileUtils.writeStringToFile(myTemp, json);

        s.s3Bucket = bucket;
        s.source = "src/test";
        s.recursive = true;

        Validator validator = new Validator();
        validator.setIncludeRegex(".*\\.json");

        s.validators = Arrays.asList(validator);

        s.execute();
    }

    @Test
    public void testValidJsonValidation() throws MojoExecutionException, IOException {

        S3UploadMojo s = new S3UploadMojo();
        String json = "{ \"glossary\": { \"title\": \"example glossary\", \"GlossDiv\": { \"title\": \"S\", \"GlossList\": { \"GlossEntry\": { \"ID\": \"SGML\", \"SortAs\": \"SGML\", \"GlossTerm\": \"Standard Generalized Markup Language\", \"Acronym\": \"SGML\", \"Abbrev\": \"ISO 8879:1986\", \"GlossDef\": { \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \"GlossSeeAlso\": [\"GML\", \"XML\"] }, \"GlossSee\": \"markup\"}}}}}";

        File myTemp = new File("src/test", UUID.randomUUID().toString() + ".json");
        myTemp.deleteOnExit(); // clean up after ourselves
        FileUtils.writeStringToFile(myTemp, json);

        s.s3Bucket = bucket;
        s.source = "src/test";
        s.recursive = true;

        Validator validator = new Validator();
        validator.setIncludeRegex(".*\\.json");

        s.validators = Arrays.asList(validator);

        s.execute();
    }
}
