package com.erigir.maven.plugin.validator;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by maxkeene on 10/13/14.
 */
public class TestJSONValidator {

    @Test
    public void testValidJSON() throws IOException, MojoExecutionException {
        JSONValidator jsonValidator = new JSONValidator();
        String jsonString =
                "{\n" +
                "    \"array\": [\n" +
                "        {\n" +
                "            \"key1\": \"value1\",\n" +
                "            \"key2\": \"value2\"\n" +
                "        },\n" +
                "        {    \n" +
                "            \"key1\": \"value3\",\n" +
                "            \"key2\": \"value4\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"map\": {\n" +
                "        \"key1\": 1,\n" +
                "        \"key2\": \"value\"\n" +
                "    }\n" +
                "}";
        File jsonFile = new File("test.json");
        FileOutputStream fos = new FileOutputStream(jsonFile);
        fos.write(jsonString.getBytes());
        fos.close();

        jsonValidator.validate(jsonFile);

        jsonFile.delete();
    }

    @Test(expected = MojoExecutionException.class)
    public void testInvalidJson() throws IOException, MojoExecutionException {
        JSONValidator jsonValidator = new JSONValidator();
        String jsonString =
                "{\n" +
                        "    \"array\": [\n" +
                        "        {\n" +
                        "            \"key1\": \"value1\",\n" +
                        "            \"key2\": \"value2\"\n" +
                        "        }\n" +
                        "        {    \n" +
                        "            \"key1\": \"value3\",\n" +
                        "            \"key2\": \"value4\"\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"map\": {\n" +
                        "        \"key1\": 1,\n" +
                        "        \"key2\": \"value\"\n" +
                        "    }\n" +
                        "}";
        File jsonFile = new File("test.json");
        FileOutputStream fos = new FileOutputStream(jsonFile);
        fos.write(jsonString.getBytes());
        fos.close();

        jsonValidator.validate(jsonFile);

        jsonFile.delete();
    }
}
