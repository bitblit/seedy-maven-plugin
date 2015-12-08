package com.erigir.maven.plugin;

import com.erigir.maven.plugin.s3uploadparam.*;
import com.erigir.wrench.drigo.JavascriptCompilation;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 Copyright 2014-2015 Christopher Weiss

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
public class TestS3UploadMojo {
    private static String BUCKET_NAME="test-seedy-bucket";



    @Test
    public void test2() throws Exception
    {
        DeployLambdaAPIMojo bean = new DeployLambdaAPIMojo();
        bean.doNotUpload = false;
        //bean.s3FilePath="2015-12-07_03-19-56-lambda-deploy.jar";
        bean.source = new File("/Users/cweiss1271/workspace/ecp-csp-test-support/target/ecp-csp-test-support-1.0.LOCAL-SNAPSHOT.jar");
        bean.configFile = new File("/Users/cweiss1271/workspace/ecp-csp-test-support/src/main/config/lambda-api-descriptor.json");
        bean.region="us-east-1";
        bean.s3Bucket="seedy-uploads";
        bean.deleteOnCompletion=true;

        bean.execute();



    }


    @Test
    @Ignore
    public void testInclusion()
            throws MojoFailureException
    {
        S3UploadMojo s = new S3UploadMojo();
        //s.doNotUpload=true;
        s.s3Bucket=BUCKET_NAME;
        s.source="src/test";
        s.recursive=true;
        s.deltaMethod = DeltaCalculationMethod.MD5;
        s.deleteNonMatch=true;
        //s.doNotUpload = true;
        s.backupCurrent=false;

        FileCompressionParam fc = new FileCompressionParam();
        fc.setIncludeRegex(".*\\.java");
        s.fileCompression = fc;

        CssCompilationParam cc = new CssCompilationParam();
        cc.setIncludeRegex(".*\\.css");
        s.cssCompilation=cc;

        BabelCompilationParam bc = new BabelCompilationParam();
        bc.setIncludeRegex(".*\\.jsx");
        s.babelCompilation=bc;

        HtmlCompressionParam hc = new HtmlCompressionParam();
        hc.setIncludeRegex(".*\\.html");
        s.htmlCompression=hc;

        JavascriptCompilationParam jc = new JavascriptCompilationParam();
        jc.setIncludeRegex(".*\\.js");
        jc.setMode(JavascriptCompilation.JSCompilationMode.CLOSURE_WHITESPACE);
        s.javascriptCompilation = jc;

        ExclusionParam exclusionParam1 = new ExclusionParam();
        exclusionParam1.setIncludeRegex(".*WEB-INF.*");

        ExclusionParam exclusionParam2 = new ExclusionParam();
        exclusionParam2.setIncludeRegex(".*dynamic_content/templates.*");
        s.exclusions = Arrays.asList(exclusionParam1,exclusionParam2);

        ProcessIncludesParam processIncludesParam = new ProcessIncludesParam();
        processIncludesParam.setIncludeRegex(".*\\.html");
        processIncludesParam.setPrefix("<!--SI:");
        processIncludesParam.setSuffix(":SI-->");
        s.processIncludes = Arrays.asList(processIncludesParam);



        ObjectMetadataSettingParam oms1 = new ObjectMetadataSettingParam();
        oms1.setIncludeRegex(".*\\.java");
        oms1.setCacheControl("Max-Age = 30");
        oms1.getUserMetaData().put("mykey", "myval");
        oms1.setContentType("text/java");

        s.objectMetadataSettings = Arrays.asList(oms1);

        ProcessReplaceParam prp = new ProcessReplaceParam();
        prp.setIncludeRegex(".*");
        prp.setPrefix("//--REP:");
        prp.setSuffix(":REP--//");
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(".*","//REPLACED!");
        prp.setReplace(map);
        s.replacement=prp;

        s.execute();

    }
}

// pre-replace
//--REP:Replace me please:REP--//
// post-replace
