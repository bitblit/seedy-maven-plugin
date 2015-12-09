package com.erigir.maven.plugin;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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

@Mojo(name = "start-new-environment", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartNewEnvironmentMojo extends AbstractSeedyMojo {
    private static final int MAX_ENVIRONMENT_NAME_LENGTH = 23; // This comes from AMZN

    /**
     * If deploying to a different account, this is the ARN of the role in that account
     * with privs to execute the deployment
     */
    @Parameter(property = "flip-environment.assumedRoleArn")
    String assumedRoleArn;

    /**
     * If deploying to a different account, this is the external ID set on the role in that account
     * with privs to execute the deployment
     */
    @Parameter(property = "flip-environment.assumedRoleExternalId")
    String assumedRoleExternalId;

    /**
     * S3 Bucket to hold the WAR file
     */
    @Parameter(property = "start-new-environment.s3Bucket", required = true)
    private String s3Bucket;

    /**
     * S3 Prefix to hold the WAR file
     */
    @Parameter(property = "start-new-environment.s3Prefix", required = true)
    private String s3Prefix;

    /**
     * Application Name in elastic beanstalk
     */
    @Parameter(property = "start-new-environment.applicationFile", required = true)
    private String applicationFile;

    /**
     * Application Name in elastic beanstalk
     */
    @Parameter(property = "start-new-environment.applicationName", required = true)
    private String applicationName;

    /**
     * Pool config file
     */
    @Parameter(property = "start-new-environment.poolConfigFile", defaultValue = "src/main/config/live-config.json")
    private String poolConfigFile;

    /**
     * Machine type
     */
    @Parameter(property = "start-new-environment.solutionStack", defaultValue = "Tomcat 8 Java 8 on 64bit Amazon Linux 2015.03 v1.4.5")
    private String solutionStack;

    /**
     * Seconds to sleep before beginning pings
     */
    @Parameter(property = "start-new-environment.prePingSleepSeconds", defaultValue = "300")
    private int prePingSleepSeconds;

    /**
     * Seconds to sleep after green
     */
    @Parameter(property = "start-new-environment.afterGreenSleepSeconds", defaultValue = "30")
    private int afterGreenSleepSeconds;

    /**
     * Max Seconds to wait before giving up
     */
    @Parameter(property = "start-new-environment.maxWaitSeconds", defaultValue = "420")
    private int maxWaitSeconds;

    /**
     * Machine type
     */
    @Parameter(property = "start-new-environment.liveServerDomainName", required = true)
    private String liveServerDomainName;

    /**
     * Max Seconds to wait before giving up
     */
    @Parameter(property = "start-new-environment.preFlipLiveWaitSeconds", defaultValue = "15")
    private int preFlipLiveWaitSeconds;

    /**
     * Machine type
     */
    @Parameter(property = "start-new-environment.terminateOldEnviroment", defaultValue = "false")
    private boolean terminateOldEnviroment;

    @Override
    public void execute() throws MojoFailureException {
        String buildId = buildId();
        int buildNumber = buildNumber(1);
        String environmentName = applicationName + "-" + buildNumber;
        String appVersionLabel = applicationName + "-" + buildId;

        if (environmentName.length() > MAX_ENVIRONMENT_NAME_LENGTH) {
            getLog().warn("Environment name '" + environmentName + " would be longer than the max " + MAX_ENVIRONMENT_NAME_LENGTH + " allowed, aborting");
            throw new MojoFailureException("Environment name '" + environmentName + " would be longer than the max " + MAX_ENVIRONMENT_NAME_LENGTH + " allowed, aborting");
        }

        AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient(credentials());
        S3Location warLocation = uploadPackageFile();

        getLog().info("Current available solution stacks: " + eb.listAvailableSolutionStacks());

        CreateApplicationVersionRequest cavr = new CreateApplicationVersionRequest().withApplicationName(applicationName).withDescription(applicationName + " build " + buildId)
                .withVersionLabel(appVersionLabel).withSourceBundle(warLocation);
        getLog().info("Creating new elastic beanstalk version " + cavr);
        eb.createApplicationVersion(cavr);

        // Create a new environment for that version
        Collection<ConfigurationOptionSetting> settings = loadSettingsFromFile();
        getLog().info("settings:" + settings);

        CreateEnvironmentRequest cer = new CreateEnvironmentRequest().withApplicationName(applicationName).withVersionLabel(appVersionLabel).withEnvironmentName(environmentName)
                .withSolutionStackName(solutionStack).withCNAMEPrefix(environmentName).withOptionSettings(settings);
        getLog().info("Creating new environment " + cer);
        eb.createEnvironment(cer);

        getLog().info("Sleeping " + prePingSleepSeconds + " seconds before we even begin polling");
        safeSleep(prePingSleepSeconds * 1000);

        getLog().info("Now waiting for either a 200 response or " + maxWaitSeconds + " have passed");
        String newEnvironmentUrl = environmentName + ".elasticbeanstalk.com";
        waitForUrl(newEnvironmentUrl, maxWaitSeconds * 1000);

        getLog().info("Now waiting until the environment is green");
        waitForEnvironmentGreen(eb, environmentName, maxWaitSeconds * 1000);

        getLog().info("Waiting " + preFlipLiveWaitSeconds + " seconds more just to make sure its ready");
        safeSleep(preFlipLiveWaitSeconds * 1000);

        getLog().info("--Should run integration tests here--");

        getLog().info("Finding current live url");
        EnvironmentDescription liveEnvironment = findEnvironmentByCNAME(eb, liveServerDomainName);

        /*
        getLog().info("Swapping new version live (old live is " + liveEnvironment + ")");
        SwapEnvironmentCNAMEsRequest swap = new SwapEnvironmentCNAMEsRequest().withSourceEnvironmentName(liveEnvironment.getEnvironmentName()).withDestinationEnvironmentName(environmentName);
        eb.swapEnvironmentCNAMEs(swap);

        if (terminateOldEnviroment) {
            getLog().info("Waiting " + preFlipLiveWaitSeconds + " seconds post live before terminating old environment");
            safeSleep(preFlipLiveWaitSeconds * 1000);

            getLog().info("Terminating old environment");
            TerminateEnvironmentRequest ter = new TerminateEnvironmentRequest().withEnvironmentName(liveEnvironment.getEnvironmentName());
            eb.terminateEnvironment(ter);
        }
        */

        getLog().info("Deploy is now complete");
    }

    // Create a new environment for that version
    private List<ConfigurationOptionSetting> loadSettingsFromFile()
            throws MojoFailureException {
        List<ConfigurationOptionSetting> rval = new LinkedList<ConfigurationOptionSetting>();

        if (poolConfigFile != null) {
            File file = new File(poolConfigFile);
            if (file.exists()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();

                    List<ConfigurationOptionSettingBridge> temp = mapper.readValue(file, new TypeReference<List<ConfigurationOptionSettingBridge>>() {
                    });
                    for (ConfigurationOptionSettingBridge c : temp) {
                        rval.add(new ConfigurationOptionSetting().withNamespace(c.namespace).withOptionName(c.optionName).withValue(c.value));
                    }
                } catch (IOException ioe) {
                    throw new MojoFailureException("Error trying to read live settings file " + file, ioe);
                }

            } else {
                getLog().info("Pool config file doesn't exist - skipping (was " + file + ")");
            }
        }

        return rval;
    }


    private S3Location uploadPackageFile()
            throws MojoFailureException {
        AmazonS3 s3 = s3();
        TransferManager mgr = new TransferManager(s3);

        // Upload the WAR file to S3
        UploadResult ur = null;
        try {
            File warFile = new File(applicationFile);
            if (!warFile.exists()) {
                throw new MojoFailureException("File " + warFile + " doesn't exist - aborting");
            }
            getLog().info("Uploading " + warFile + " (" + warFile.length() + " bytes) to " + s3Bucket + "/" + s3Prefix);
            ur = mgr.upload(s3Bucket, s3Prefix, warFile).waitForUploadResult();
            getLog().info("Upload finished");
        } catch (InterruptedException ie) {
            getLog().info("Interrupted during upload");
        }

        // Create a new EB Version
        S3Location warLocation = new S3Location().withS3Bucket(ur.getBucketName()).withS3Key(ur.getKey());
        return warLocation;
    }

    private EnvironmentDescription findEnvironmentByName(AWSElasticBeanstalkClient eb, String environmentName) {
        getLog().info("Finding environment " + environmentName);
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        EnvironmentDescription rval = null;
        for (EnvironmentDescription ed : res.getEnvironments()) {
            if (ed.getEnvironmentName().equals(environmentName)) {
                getLog().debug("Found match " + ed);
                if (rval == null) {
                    rval = ed;
                } else {
                    getLog().warn("Found multiple matches, using newer");

                    if (ed.getDateUpdated().after(rval.getDateUpdated())) {
                        rval = ed;
                    }
                }
            }
        }

        getLog().info("Found environment " + rval + " for name " + environmentName);
        return rval;
    }

    private EnvironmentDescription findEnvironmentByCNAME(AWSElasticBeanstalkClient eb, String domainName) {
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        EnvironmentDescription rval = null;
        for (EnvironmentDescription ed : res.getEnvironments()) {
            if (ed.getCNAME().equals(domainName)) {
                rval = ed;
            }
        }
        return rval;
    }

    private String getEnvironmentColor(AWSElasticBeanstalkClient eb, String environmentName)
            throws MojoFailureException {

        EnvironmentDescription ed = findEnvironmentByName(eb, environmentName);

        if (ed == null) {
            getLog().info("Couldnt find environment with name " + environmentName);
            throw new MojoFailureException("Couldnt find environment with name " + environmentName);
        } else {
            String rval = ed.getHealth();
            getLog().info("Returning " + rval);
            return rval;
        }
    }

    public void waitForEnvironmentGreen(AWSElasticBeanstalkClient eb, String environmentName, long maxWaitMS)
            throws MojoFailureException {
        long startTime = System.currentTimeMillis();
        long step = 20000;
        long timeoutAt = startTime + maxWaitMS;

        getLog().info("Environment is " + environmentName + " started waiting for green at " + new Date());
        String color = "NULL";

        while (!"Green".equalsIgnoreCase(color) && System.currentTimeMillis() < timeoutAt) {
            color = getEnvironmentColor(eb, environmentName);
            if (!"Green".equalsIgnoreCase(color)) {
                long elapsed = System.currentTimeMillis() - startTime;
                getLog().info(environmentName + " was not green, waiting " + step + "ms " + elapsed + "ms have elapsed");
                safeSleep(step);
            }
        }

        if (!"Green".equalsIgnoreCase(color)) {
            getLog().info(environmentName + " not green in " + maxWaitMS + "ms, giving up");
            throw new MojoFailureException(environmentName + " not green in " + maxWaitMS + "ms, giving up");
        }

        getLog().info(environmentName + " is now green");
    }

    private void waitForUrl(String url, long maxWaitMS)
            throws MojoFailureException {
        long startTime = System.currentTimeMillis();
        long timeoutAt = startTime + maxWaitMS;
        long stepTime = 20000; // 20 second steps
        getLog().info("URL is http://" + url + " started at " + new Date());
        URL u = null;
        try {
            u = new URL("http://" + url);
        } catch (MalformedURLException mue) {
            throw new MojoFailureException("Bad URL " + url);
        }

        boolean found = false;

        while (!found && System.currentTimeMillis() < timeoutAt) {
            try {
                URLConnection uc = u.openConnection();
                found = true;
            } catch (IOException ioe) {
                long elapsed = System.currentTimeMillis() - startTime;
                getLog().info(url + " Not found, waiting " + stepTime + "ms " + elapsed + "ms already elapsed");
            }
            safeSleep(stepTime);
        }

        if (!found) {
            getLog().info("Timed out waiting for URL " + url);
            throw new MojoFailureException("Timed out");
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

    /**
     * Ok, I must be doing this wrong, but I can't find anything in their docs for reading the
     * standard JSON file with these, so here I go
     */
    static class ConfigurationOptionSettingBridge {
        @JsonProperty("OptionName")
        private String optionName;
        @JsonProperty("Value")
        private String value;
        @JsonProperty("Namespace")
        private String namespace;

        public String getOptionName() {
            return optionName;
        }

        public void setOptionName(String optionName) {
            this.optionName = optionName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }

}

