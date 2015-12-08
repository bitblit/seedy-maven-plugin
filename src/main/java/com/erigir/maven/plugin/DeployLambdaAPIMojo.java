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
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.erigir.maven.plugin.apiconfig.APIConfig;
import com.erigir.maven.plugin.apiconfig.EndpointConfig;
import com.erigir.maven.plugin.apiconfig.LambdaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
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
 *
 * Still, I'm releasing now (12/8/15) so that I can at least do multiple lambda-function
 * deploys from a single uploaded JAR file, which will simplify my conversion from local
 * based stuff quite a lot (and save me lots of upload time)
 *
 *
 * This Mojo searches the packages of a specified Jar file, finds all
 * functions annotated with @APIGatewayLambdaDescriptor, uploads the jar file 
 * to S3, deploys all the lambda functions, and wires (or rewires) them into
 * the specified AWS API Gateway
 *
 * Note that this expects that you have already combined your dependencies into a
 * single jar - use another tool (such as the shade plugin) to do this before
 * running this mojo
 */
@Mojo(name = "deploy-lambda-api", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployLambdaAPIMojo extends AbstractSeedyMojo {

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
            AmazonApiGateway apiGateway = apiGateway();
            AWSLambdaClient lambda = lambda();

            APIConfig config = readConfigFile();
            String newFileName = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date())+"-lambda-deploy.jar";

            if (doNotUpload) {
                getLog().info("Processing finished, doNotUpload specified.");
                getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run) and processed %d functions",
                        source, s3Bucket, newFileName, config.getEndpoints().size()));
                return;
            } else {

                if (config.getEndpoints().size() > 0) {
                    if (s3FilePath==null) {
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
                                    if (newPercent!=percent)
                                    {
                                        percent=(int)newPercent;
                                        getLog().info("Transferred "+total+" bytes of "+source.length()+" : "+percent+"% complete");
                                    }
                                }

                            }
                        });
                        s3().putObject(por);

                        getLog().info("File uploaded successfully");
                    }
                    else
                    {
                        getLog().info("Using explicit s3 path : "+s3FilePath);
                        newFileName = s3FilePath;
                    }

                    RestApi api = findApi(apiGateway, config.getApiName());
                    List<Resource> startingResources = findResources(apiGateway, api); // will be used by api gateway
                    getLog().info("Found "+startingResources+" resources in the current API Gateway");


                    for (EndpointConfig e : config.getEndpoints()) {
                        getLog().info("Saving Lambda function "+ e.getLambdaConfig().getFunctionName());
                        deployLambdaFunction(lambda, e, newFileName);


                        getLog().info("WARNING: Skipping API gateway creation since it isn't fully baked yet");
                        //getLog().info("Saving API Gateway resource "+ e.getApiGatewayConfig().getResourcePath()+ " "+ e.getApiGatewayConfig().getResourceMethods());
                        //deployGatewayEndpoint(apiGateway, api, startingResources,  e);
                        // TODO: Saving gateway resource
                    }

                    if (deleteOnCompletion)
                    {
                        getLog().info("DeleteOnCompletion is specified - removing file "+newFileName);
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

    private APIConfig readConfigFile()
            throws MojoFailureException
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            APIConfig config = objectMapper.readValue(configFile, APIConfig.class);

            for (EndpointConfig e:config.getEndpoints())
            {
                // TODO: Pretest the class names, methods, etc
            }

            return config;
        } catch (IOException e)
        {
            throw new MojoFailureException("Error trying to read config file",e);
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

    private AmazonApiGateway apiGateway()
            throws MojoFailureException {
        return new AmazonApiGatewayClient(credentials());
    }

    private AWSLambdaClient lambda() throws MojoFailureException{
        AWSLambdaClient client =  new AWSLambdaClient(credentials());
        client.setRegion(region());
        return client;
    }

    private Region region()
    {
        return Region.getRegion(Regions.fromName(region));
    }

    private Resource findResourceByPath(List<Resource> resources, String path)
    {
        Optional<Resource> atStart = resources.stream().filter((p)->p.getPath().equals(path)).findFirst();
        return (atStart.isPresent())?atStart.get():null;
    }

    private void deployGatewayEndpoint(AmazonApiGateway apiGateway, RestApi api, List<Resource> resourcesAtStart, final EndpointConfig endpointConfig)
    throws MojoFailureException
    {
        try
        {
            Resource atStart = findResourceByPath(resourcesAtStart, endpointConfig.getApiGatewayConfig().getResourcePath());

            if (atStart!=null)
            {
                getLog().debug("Attempting delete of resource "+endpointConfig.getApiGatewayConfig().getResourcePath());
                DeleteResourceRequest del = new DeleteResourceRequest()
                        .withResourceId(atStart.getId())
                        .withRestApiId(api.getId());
                apiGateway.deleteResource(del);
                getLog().debug("Delete succeeded");

            }
            Resource parentResource = findParent(resourcesAtStart, endpointConfig);
            getLog().info("Creating resource "+endpointConfig.getApiGatewayConfig().getResourcePath());

            CreateResourceRequest create = new CreateResourceRequest();
            create.withParentId(parentResource.getId());
            create.withRestApiId(api.getId());
            create.withPathPart(endpointConfig.getApiGatewayConfig().endPathPart());
            CreateResourceResult createResult = apiGateway.createResource(create);

            getLog().info("Created, Result was "+createResult.getId());

            for (String method:endpointConfig.getApiGatewayConfig().getResourceMethods())
            {
                getLog().info("Creating method "+method);


                PutIntegrationRequest intRequest = new PutIntegrationRequest();
                intRequest.withCacheKeyParameters();
                //intRequest.withCacheNamespace(); TODO: Impl
                //intRequest.withCredentials(); TODO: Impl
                intRequest.withHttpMethod(method);
                //intRequest.withIntegrationHttpMethod(); TODO: Impl
                //intRequest.withRequestParameters(); TODO: Impl
                //intRequest.withRequestTemplates(); TODO: Impl
                intRequest.withResourceId(createResult.getId());
                intRequest.withRestApiId(api.getId());
                intRequest.withType(IntegrationType.AWS);
                // intRequest.withUri(); TODO: Impl

                PutIntegrationResult methodResult = apiGateway.putIntegration(intRequest);

                /*
                PutMethodRequest putMethodRequest = new PutMethodRequest();
                putMethodRequest.withApiKeyRequired(false); // todo: implement
                putMethodRequest.withAuthorizationType("NONE"); // todo: implement
                putMethodRequest.withHttpMethod(method);
                //putMethodRequest.withRequestModels();
                //putMethodRequest.withRequestParameters();
                putMethodRequest.withResourceId(createResult.getId());
                putMethodRequest.withRestApiId(api.getId());

                PutMethodResult methodResult = apiGateway.putMethod(putMethodRequest);
                */
                getLog().info("Method created "+methodResult);
            }



        }
        catch (AmazonClientException ace)
        {
            throw new MojoFailureException("Failure" , ace);
        }


    }

    private Resource findParent(List<Resource> resource, EndpointConfig config)
            throws MojoFailureException
    {

        Resource res = findResourceByPath(resource, config.getApiGatewayConfig().parentPath());

        if (res==null)
        {
            throw new MojoFailureException("Couldnt find parent for "+config.getApiGatewayConfig().getResourcePath());
        }
        return res;
    }

    private List<Resource> findResources(AmazonApiGateway apiGateway, RestApi api)
    {
        GetResourcesResult resourcesResult = apiGateway.getResources(new GetResourcesRequest().withRestApiId(api.getId()));
        return resourcesResult.getItems();
    }


    private RestApi findApi(AmazonApiGateway apiGateway, String apiName)
            throws MojoFailureException
    {
        RestApi rval = null;
        List<String> names = new LinkedList<>();

        Objects.requireNonNull(apiName);

        GetRestApisResult result = apiGateway.getRestApis(new GetRestApisRequest());
        for (RestApi r:result.getItems())
        {
            names.add(r.getName());
            if (apiName.equals(r.getName()))
            {
                rval = r;
            }
        }

        if (rval==null)
        {
            throw new MojoFailureException("Couldn't find api with name "+apiName+" valid were "+names);
        }

        return rval;
    }

        /**
         * Attempts to delete an existing function of the same name then deploys the
         * function code to AWS Lambda. TODO: Attempt to do an update with
         * versioning if the function already TODO: exists.
         */
    private void deployLambdaFunction(AWSLambdaClient lambda, EndpointConfig endpointConfig, String s3FilePath) {
        // Attempt to delete it first
        try {
            DeleteFunctionRequest deleteRequest = new DeleteFunctionRequest();
            // Why the hell didn't they make this a static method?
            deleteRequest = deleteRequest.withFunctionName(endpointConfig.getLambdaConfig().getFunctionName());
            lambda.deleteFunction(deleteRequest);
        } catch (Exception ignored) {
            // function didn't exist in the first place.
            getLog().debug("Didnt delete function "+endpointConfig.getLambdaConfig().getFunctionName());
        }

        CreateFunctionResult result = createFunction(lambda, endpointConfig, s3FilePath);

        getLog().info("Function deployed: " + result.getFunctionArn());
    }

    /**
     * Makes a create function call on behalf of the caller, deploying the
     * function code to AWS lambda.
     *
     * @return A CreateFunctionResult indicating the success or failure of the
     *         request.
     */
    private CreateFunctionResult createFunction(AWSLambdaClient lambda, EndpointConfig endpointConfig, String s3FilePath) {
        CreateFunctionRequest createFunctionRequest = new CreateFunctionRequest();
        createFunctionRequest.setDescription(endpointConfig.getDescription());
        createFunctionRequest.setRole(endpointConfig.getLambdaConfig().getRoleArn());
        createFunctionRequest.setFunctionName(endpointConfig.getLambdaConfig().getFunctionName());
        createFunctionRequest.setHandler(endpointConfig.getClassName()+"::handleRequest"); // For now, just handles the lambda cast one
        createFunctionRequest.setRuntime(endpointConfig.getLambdaConfig().getRuntime());
        createFunctionRequest.setTimeout(endpointConfig.getLambdaConfig().getTimeoutInSeconds());
        createFunctionRequest.setMemorySize(endpointConfig.getLambdaConfig().getMemoryInMb());

        FunctionCode functionCode = new FunctionCode();
        functionCode.setS3Bucket(s3Bucket);
        functionCode.setS3Key(s3FilePath);
        createFunctionRequest.setCode(functionCode);

        return lambda.createFunction(createFunctionRequest);
    }

    private FileInputStream silentOpenInputStream(File file)
    {
        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException fnf)
        {
            throw new RuntimeException("Like this could be handled at runtime?",fnf);
        }
    }

}
