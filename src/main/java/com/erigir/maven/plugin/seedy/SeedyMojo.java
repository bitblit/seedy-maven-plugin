package com.erigir.maven.plugin.seedy;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Mojo(name = "seedy")
public class SeedyMojo extends AbstractMojo
{
    private static final int MAX_ENVIRONMENT_NAME_LENGTH=23; // This comes from AMZN
  /** S3 Bucket to hold the WAR file */
  @Parameter(property = "seedy.s3Bucket", required = true)
  private String s3Bucket;

    /** S3 Prefix to hold the WAR file */
    @Parameter(property = "seedy.s3Prefix", required = true)
    private String s3Prefix;

    /** Application Name in elastic beanstalk */
    @Parameter(property = "seedy.applicationFile", required = true)
    private String applicationFile;

    /** Application Name in elastic beanstalk */
    @Parameter(property = "seedy.applicationName", required = true)
    private String applicationName;

    /** Pool config file */
    @Parameter(property = "seedy.poolConfigFile", defaultValue = "src/main/config/live-config.json")
    private String poolConfigFile;

    /** Machine type */
    @Parameter(property = "seedy.solutionStack", defaultValue = "Tomcat 7 Java 7 on 64bit Amazon Linux 2014.03 v1.0.4")
    private String solutionStack;

    /** Seconds to sleep before beginning pings */
    @Parameter(property = "seedy.prePingSleepSeconds", defaultValue = "300")
    private int prePingSleepSeconds;

    /** Seconds to sleep after green */
    @Parameter(property = "seedy.afterGreenSleepSeconds", defaultValue = "30")
    private int afterGreenSleepSeconds;

    /** Max Seconds to wait before giving up */
    @Parameter(property = "seedy.maxWaitSeconds", defaultValue = "420")
    private int maxWaitSeconds;

    /** Machine type */
    @Parameter(property = "seedy.liveServerDomainName", required = true)
    private String liveServerDomainName;

    /** Max Seconds to wait before giving up */
    @Parameter(property = "seedy.preFlipLiveWaitSeconds", defaultValue = "15")
    private int preFlipLiveWaitSeconds;

    /** Machine type */
    @Parameter(property = "seedy.terminateOldEnviroment", defaultValue="false")
    private boolean terminateOldEnviroment;

    @Override
  public void execute() throws MojoExecutionException
  {
      String buildId = buildId();
      int buildNumber = buildNumber();
      String environmentName = applicationName+"-"+buildNumber;
      String appVersionLabel = applicationName+"-"+buildId;

      if (environmentName.length()>MAX_ENVIRONMENT_NAME_LENGTH)
      {
          getLog().warn("Environment name '"+environmentName+" would be longer than the max "+MAX_ENVIRONMENT_NAME_LENGTH+" allowed, aborting");
          throw new MojoExecutionException("Environment name '"+environmentName+" would be longer than the max "+MAX_ENVIRONMENT_NAME_LENGTH+" allowed, aborting");
      }

      getLog().info("Fetching AWS Credentials");
      AWSCredentials credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
      if (credentials==null)
      {
          throw new MojoExecutionException("Couldn't fetch credentials - either set aws.accessKeyId and aws.secretKey or (better) grant this machine an IAM role");
      }

      getLog().info("Using credentials : "+obscure(credentials.getAWSAccessKeyId(),2)+" / "+obscure(credentials.getAWSSecretKey(),2));

      AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient(credentials);
      S3Location warLocation = uploadPackageFile(credentials);

      getLog().info("Current available solution stacks: "+eb.listAvailableSolutionStacks());

      CreateApplicationVersionRequest cavr = new CreateApplicationVersionRequest().withApplicationName(applicationName).withDescription(applicationName+" build "+buildId)
              .withVersionLabel(appVersionLabel).withSourceBundle(warLocation);
      getLog().info("Creating new elastic beanstalk version "+cavr);
      eb.createApplicationVersion(cavr);

      // Create a new environment for that version
      Collection<ConfigurationOptionSetting> settings = loadSettingsFromFile();
      getLog().info("settings:"+settings);

      CreateEnvironmentRequest cer = new CreateEnvironmentRequest().withApplicationName(applicationName).withVersionLabel(appVersionLabel).withEnvironmentName(environmentName)
              .withSolutionStackName(solutionStack).withCNAMEPrefix(environmentName).withOptionSettings(settings);
      getLog().info("Creating new environment "+cer);
      eb.createEnvironment(cer);

      getLog().info("Sleeping "+prePingSleepSeconds+" seconds before we even begin polling");
      safeSleep(prePingSleepSeconds*1000);

      getLog().info("Now waiting for either a 200 response or "+maxWaitSeconds+" have passed");
      String newEnvironmentUrl = environmentName+".elasticbeanstalk.com";
      waitForUrl(newEnvironmentUrl,maxWaitSeconds*1000);

      getLog().info("Now waiting until the environment is green");
      waitForEnvironmentGreen(eb,environmentName,maxWaitSeconds*1000);

      getLog().info("Waiting "+preFlipLiveWaitSeconds+" seconds more just to make sure its ready");
      safeSleep(preFlipLiveWaitSeconds*1000);

      getLog().info("--Should run integration tests here--");

      getLog().info("Finding current live url");
      EnvironmentDescription liveEnvironment = findEnvironmentByCNAME(eb, liveServerDomainName);

      getLog().info("Swapping new version live (old live is "+liveEnvironment+")");
      SwapEnvironmentCNAMEsRequest swap = new SwapEnvironmentCNAMEsRequest().withSourceEnvironmentName(liveEnvironment.getEnvironmentName()).withDestinationEnvironmentName(environmentName);
      eb.swapEnvironmentCNAMEs(swap);

      if (terminateOldEnviroment) {
          getLog().info("Waiting " + preFlipLiveWaitSeconds + " seconds post live before terminating old environment");
          safeSleep(preFlipLiveWaitSeconds * 1000);

          getLog().info("Terminating old environment");
          TerminateEnvironmentRequest ter = new TerminateEnvironmentRequest().withEnvironmentName(liveEnvironment.getEnvironmentName());
          eb.terminateEnvironment(ter);
      }

      getLog().info("Deploy is now complete");
  }

    // Create a new environment for that version
    private List<ConfigurationOptionSetting> loadSettingsFromFile()
    throws MojoExecutionException
    {
        List<ConfigurationOptionSetting> rval = new LinkedList<ConfigurationOptionSetting>();

        if (poolConfigFile!=null)
        {
            File file = new File(poolConfigFile);
            if (file.exists())
            {
                try {
                    ObjectMapper mapper = new ObjectMapper();

                    List<ConfigurationOptionSettingBridge> temp = mapper.readValue(file, new TypeReference<List<ConfigurationOptionSettingBridge>>() {
                    });
                    for (ConfigurationOptionSettingBridge c:temp)
                    {
                        rval.add(new ConfigurationOptionSetting().withNamespace(c.namespace).withOptionName(c.optionName).withValue(c.value));
                    }
                } catch(IOException ioe)
                {
                    throw new MojoExecutionException("Error trying to read live settings file "+file,ioe);
                }

            }
            else
            {
                getLog().info("Pool config file doesn't exist - skipping (was "+file+")");
            }
        }

        return rval;
    }


    private S3Location uploadPackageFile(AWSCredentials credentials)
            throws MojoExecutionException
    {
        AmazonS3Client s3 = new AmazonS3Client(credentials);
        TransferManager mgr = new TransferManager(s3);

        // Upload the WAR file to S3
        UploadResult ur = null;
        try {
            File warFile = new File(applicationFile);
            if (!warFile.exists())
            {
                throw new MojoExecutionException("File "+warFile+" doesn't exist - aborting");
            }
            getLog().info("Uploading " + warFile + " ("+warFile.length()+" bytes) to " + s3Bucket + "/" + s3Prefix);
            ur = mgr.upload(s3Bucket, s3Prefix, warFile).waitForUploadResult();
            getLog().info("Upload finished");
        }
        catch (InterruptedException ie)
        {
            getLog().info("Interrupted during upload");
        }

        // Create a new EB Version
        S3Location warLocation = new S3Location().withS3Bucket(ur.getBucketName()).withS3Key(ur.getKey());
        return warLocation;
    }

    private int buildNumber()
    {
        Integer rval = null;
        String env = System.getenv("BUILD_NUMBER");
        if (env==null)
        {
            getLog().info("No environment variable 'BUILD_NUMBER' found, defaulting to 1 (Jenkins would set this)");
            rval = 1;
        }
        else
        {
            rval = new Integer(env);
        }
        return rval;
    }

    private String buildId()
    {
        String rval = System.getenv("BUILD_ID");
        if (rval==null)
        {
            getLog().info("No environment variable 'BUILD_ID' found, defaulting to timestamp (Jenkins would set this)");
            rval = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
        }
        return rval;
    }

    private EnvironmentDescription findEnvironmentByName(AWSElasticBeanstalkClient eb,String environmentName)
    {
        getLog().info("Finding environment "+environmentName);
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        EnvironmentDescription rval = null;
        for (EnvironmentDescription ed:res.getEnvironments())
        {
            if (ed.getEnvironmentName().equals(environmentName))
            {
                getLog().debug("Found match " + ed);
                if (rval==null)
                {
                    rval = ed;
                }
                else
                {
                    getLog().warn("Found multiple matches, using newer");

                    if (ed.getDateUpdated().after(rval.getDateUpdated()))
                    {
                        rval = ed;
                    }
                }
            }
        }

        getLog().info("Found environment "+rval+" for name "+environmentName);
        return rval;
    }

    private EnvironmentDescription findEnvironmentByCNAME(AWSElasticBeanstalkClient eb,String domainName)
    {
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        EnvironmentDescription rval = null;
        for (EnvironmentDescription ed:res.getEnvironments())
        {
            if (ed.getCNAME().equals(domainName))
            {
                rval = ed;
            }
        }
        return rval;
    }

    private String getEnvironmentColor(AWSElasticBeanstalkClient eb,String environmentName)
            throws MojoExecutionException
    {

        EnvironmentDescription ed = findEnvironmentByName(eb,environmentName);

        if (ed==null)
        {
            getLog().info("Couldnt find environment with name "+environmentName);
            throw new MojoExecutionException("Couldnt find environment with name "+environmentName);
        }
        else
        {
            String rval = ed.getHealth();
            getLog().info("Returning "+rval);
            return rval;
        }
    }

    public void waitForEnvironmentGreen(AWSElasticBeanstalkClient eb,String environmentName,long maxWaitMS)
            throws MojoExecutionException
    {
        long startTime = System.currentTimeMillis();
        long step = 20000;
        long timeoutAt = startTime+maxWaitMS;

        getLog().info("Environment is "+environmentName+" started waiting for green at "+new Date());
        String color = "NULL";

        while (!"Green".equalsIgnoreCase(color) && System.currentTimeMillis()<timeoutAt)
        {
            color = getEnvironmentColor(eb, environmentName);
            if (!"Green".equalsIgnoreCase(color))
            {
                long elapsed = System.currentTimeMillis()-startTime;
                getLog().info(environmentName+" was not green, waiting "+step+"ms "+elapsed+"ms have elapsed");
                safeSleep(step);
            }
        }

        if (!"Green".equalsIgnoreCase(color))
        {
            getLog().info(environmentName+" not green in "+maxWaitMS+"ms, giving up");
            throw new MojoExecutionException(environmentName+" not green in "+maxWaitMS+"ms, giving up");
        }

        getLog().info(environmentName+" is now green");
    }

    private void waitForUrl(String url,long maxWaitMS)
            throws MojoExecutionException {
        long startTime = System.currentTimeMillis();
        long timeoutAt = startTime + maxWaitMS;
        long stepTime = 20000; // 20 second steps
        getLog().info("URL is http://" + url + " started at " + new Date());
        URL u = null;
        try
        {
            u =new URL("http://"+url);
        }
        catch (MalformedURLException mue)
        {
            throw new MojoExecutionException("Bad URL "+url);
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
            throw new MojoExecutionException("Timed out");
        }
    }

    private void safeSleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ie)
        {
            getLog().info("Sleep interrupted");
        }
    }

    private String obscure(String input, int save)
    {
        String rval = input;
        if (input!=null && input.length()>save*2)
        {
            StringBuilder b = new StringBuilder();
            b.append(input.substring(0,save));
            while (b.length()<input.length()-save)
            {
                b.append("*");
            }
            b.append(input.substring(input.length()-save));
            rval = b.toString();
        }
        return rval;
    }

    /**
     * Ok, I must be doing this wrong, but I can't find anything in their docs for reading the
     * standard JSON file with these, so here I go
     */
    static class ConfigurationOptionSettingBridge
    {
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

