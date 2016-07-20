package com.erigir.maven.plugin;

import com.amazonaws.AmazonClientException;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.model.*;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.LambdaConfiguration;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.erigir.maven.plugin.apiconfig.LambdaConfig;
import com.erigir.maven.plugin.apiconfig.MultiLambdaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Copyright 2014-2015 Christopher Weiss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

/**
 * Important note : This mojo isn't complete yet, and can't be, really, until AWS
 * releases some more classes into the API (specifically, the ability to do a
 * PutIntegrationRequest against APIGateway that links to a Lambda function, and to
 * map it together)
 * <p>
 * Still, I'm releasing now (12/8/15) so that I can at least do multiple lambda-function
 * deploys from a single uploaded JAR file, which will simplify my conversion from local
 * based stuff quite a lot (and save me lots of upload time)
 * <p>
 * <p>
 * This Mojo searches the packages of a specified Jar file, finds all
 * functions annotated with @APIGatewayLambdaDescriptor, uploads the jar file
 * to S3, deploys all the lambda functions, and wires (or rewires) them into
 * the specified AWS API Gateway
 * <p>
 * Note that this expects that you have already combined your dependencies into a
 * single jar - use another tool (such as the shade plugin) to do this before
 * running this mojo
 */
@Mojo(name = "deploy-multi-lambda", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMultiLambdaMojo extends AbstractSeedyMojo {

    /**
     * If deploying to a different account, this is the ARN of the role in that account
     * with privs to execute the deployment
     */
    @Parameter(property = "deploy-lambda-api.assumedRoleArn")
    String assumedRoleArn;

    /**
     * If deploying to a different account, this is the external ID set on the role in that account
     * with privs to execute the deployment
     */
    @Parameter(property = "deploy-lambda-api.assumedRoleExternalId")
    String assumedRoleExternalId;

    /**
     * Examine the jar and create a plan but don't upload or execute it
     * This can be set to true to perform a "dryRun" execution.
     */
    @Parameter(property = "deploy-lambda-api.doNotUpload", defaultValue = "false")
    boolean doNotUpload;

    /**
     * If true, deletes the uploaded Jar file from S3 after the process is completed
     */
    @Parameter(property = "deploy-lambda-api.deleteOnCompletion", defaultValue = "true")
    boolean deleteOnCompletion;

    /**
     * The Jar file to upload.
     */
    @Parameter(property = "deploy-lambda-api.source", required = true)
    File source;

    /**
     * The configuration file.
     */
    @Parameter(property = "deploy-lambda-api.config-file", required = true)
    File configFile;


    /**
     * If this parameter is set, the source file is ignored and instead the
     * mojo will start the api creation stuff from the JAR file specified
     * here.  Mainly useful for debugging your api config file when the code
     * doesn't change but the API gateway stuff does.
     */
    @Parameter(property = "deploy-lambda-api.s3FilePath", required = false)
    String s3FilePath;


    /**
     * The bucket to upload the source jar into - the file
     * will have an autogenerated name and will be deleted after the
     * process completes
     */
    @Parameter(property = "deploy-lambda-api.s3Bucket", required = true)
    String s3Bucket;

    /**
     * Force override of endpoint (Defaults to us-east-1
     */
    @Parameter(property = "deploy-lambda-api.region", defaultValue = "us-east-1")
    String region;

    @Override
    public void execute() throws MojoFailureException {
        try {
            // Setup the source file
            if (!source.exists()) {
                throw new MojoFailureException("File/folder doesn't exist: " + source);
            }

            AmazonS3 s3 = s3();
            AWSLambdaClient lambda = lambda();

            MultiLambdaConfig config = readConfigFile();
            String newFileName = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date()) + "-lambda-deploy.jar";

            if (doNotUpload) {
                getLog().info("Processing finished, doNotUpload specified.");
                getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run) and processed %d functions",
                        source, s3Bucket, newFileName, config.getFunctions().size()));
                return;
            } else {

                if (config.getFunctions().size() > 0) {
                    if (s3FilePath == null) {
                        getLog().info("Uploading JAR file to s3 (" + source.length() + " bytes : " + source.getAbsolutePath() + ")");
                        ObjectMetadata omd = new ObjectMetadata();
                        omd.setContentType("application/java");
                        omd.setContentLength(source.length());
                        PutObjectRequest por = new PutObjectRequest(s3Bucket, newFileName, silentOpenInputStream(source), omd);
                        //final AtomicLong total = new AtomicLong(0);
                        por.withGeneralProgressListener(new ProgressListener() {
                            long total = 0;
                            int percent = 0;

                            @Override
                            public void progressChanged(ProgressEvent progressEvent) {
                                if (progressEvent.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                                    total += progressEvent.getBytes();
                                    long newPercent = (total * 100 / source.length());
                                    if (newPercent != percent) {
                                        percent = (int) newPercent;
                                        getLog().info("Transferred " + total + " bytes of " + source.length() + " : " + percent + "% complete");
                                    }
                                }

                            }
                        });
                        s3().putObject(por);

                        getLog().info("File uploaded successfully");
                    } else {
                        getLog().info("Using explicit s3 path : " + s3FilePath);
                        newFileName = s3FilePath;
                    }

                    for (LambdaConfig e : config.getFunctions()) {
                        getLog().info("Saving Lambda function " + e.getFunctionName());
                        CreateFunctionResult lambdaResult = deployLambdaFunction(lambda, config, e, newFileName);
                        getLog().info("Result : "+lambdaResult);
                    }

                    if (deleteOnCompletion) {
                        getLog().info("DeleteOnCompletion is specified - removing file " + newFileName);
                        s3.deleteObject(s3Bucket, newFileName);
                    }

                    getLog().info("Process completed");
                } else {
                    getLog().info("Stopping - no functions to upload");
                }
            }

        } finally {
            getLog().info("Seedy: All processing finished.");
        }
    }

    private MultiLambdaConfig readConfigFile()
            throws MojoFailureException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MultiLambdaConfig config = objectMapper.readValue(configFile, MultiLambdaConfig.class);

            return config;
        } catch (IOException e) {
            throw new MojoFailureException("Error trying to read config file", e);
        }
    }

    @Override
    public String getAssumedRoleArn() {
        return assumedRoleArn;
    }

    @Override
    public String getAssumedRoleExternalId() {
        return assumedRoleExternalId;
    }

    private AWSLambdaClient lambda() throws MojoFailureException {
        AWSLambdaClient client = new AWSLambdaClient(credentials());
        client.setRegion(region());
        return client;
    }

    private Region region() {
        return Region.getRegion(Regions.fromName(region));
    }

    private Resource findResourceByPath(List<Resource> resources, String path) {
        Optional<Resource> atStart = resources.stream().filter((p) -> p.getPath().equals(path)).findFirst();
        return (atStart.isPresent()) ? atStart.get() : null;
    }

    /**
     * Attempts to delete an existing function of the same name then deploys the
     * function code to AWS Lambda. TODO: Attempt to do an update with
     * versioning if the function already exists.
     */
    private CreateFunctionResult deployLambdaFunction(AWSLambdaClient lambda, MultiLambdaConfig multi, LambdaConfig lambdaConfig, String s3FilePath) {
        // Attempt to delete it first
        try {
            DeleteFunctionRequest deleteRequest = new DeleteFunctionRequest();
            deleteRequest = deleteRequest.withFunctionName(multi.fullName(lambdaConfig));
            lambda.deleteFunction(deleteRequest);
        } catch (Exception ignored) {
            // function didn't exist in the first place.
            getLog().debug("Didnt delete function " + lambdaConfig.getFunctionName());
        }

        CreateFunctionResult result = createFunction(lambda, multi, lambdaConfig, s3FilePath);

        getLog().info("Function deployed: " + result.getFunctionArn());
        return result;
    }

    /**
     * Makes a create function call on behalf of the caller, deploying the
     * function code to AWS lambda.
     *
     * @return A CreateFunctionResult indicating the success or failure of the
     * request.
     */
    private CreateFunctionResult createFunction(AWSLambdaClient lambda, MultiLambdaConfig multi, LambdaConfig lambdaConfig, String s3FilePath) {
        CreateFunctionRequest createFunctionRequest = new CreateFunctionRequest();
        createFunctionRequest.setDescription(lambdaConfig.getDescription());
        createFunctionRequest.setRole(lambdaConfig.getRoleArn());
        createFunctionRequest.setFunctionName(multi.fullName(lambdaConfig));
        createFunctionRequest.setHandler(lambdaConfig.getClassName() + "::handleRequest"); // For now, just handles the lambda cast one
        createFunctionRequest.setRuntime(lambdaConfig.getRuntime());
        createFunctionRequest.setTimeout(lambdaConfig.getTimeoutInSeconds());
        createFunctionRequest.setMemorySize(lambdaConfig.getMemoryInMb());


        getLog().debug("Creating function from code in bucket :"+s3Bucket+" path "+s3FilePath);
        FunctionCode functionCode = new FunctionCode();
        functionCode.setS3Bucket(s3Bucket);
        functionCode.setS3Key(s3FilePath);
        createFunctionRequest.setCode(functionCode);

        /*

        CreateEventSourceMappingRequest cm = new CreateEventSourceMappingRequest()
                .withBatchSize()
                .withEnabled()
                .withEventSourceArn()
                .withFunctionName()
                .withStartingPosition()
                */

        return lambda.createFunction(createFunctionRequest);
    }

    private FileInputStream silentOpenInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException fnf) {
            throw new RuntimeException("Like this could be handled at runtime?", fnf);
        }
    }

}