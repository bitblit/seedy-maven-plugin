package com.erigir.maven.plugin.seedy;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

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
    @Parameter(property = "seedy.poolConfigFile", defaultValue = "live-config.json")
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

      AmazonS3Client s3 = new AmazonS3Client(credentials);
      AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient(credentials);
      TransferManager mgr = new TransferManager(s3);

      getLog().info("Current available solution stacks: "+eb.listAvailableSolutionStacks());

      // Upload the WAR file to S3
      UploadResult ur = null;
      try {
          File warFile = new File(applicationFile);
          getLog().info("Uploading " + warFile + " to " + s3Bucket + "/" + s3Prefix);
          ur = mgr.upload(s3Bucket, s3Prefix, warFile).waitForUploadResult();
          getLog().info("Upload finished");
      }
        catch (InterruptedException ie)
          {
              getLog().info("Interrupted during upload");
          }

      // Create a new EB Version
      S3Location warLocation = new S3Location().withS3Bucket(ur.getBucketName()).withS3Key(ur.getKey());
      String appVersionLabel = applicationName+"-"+buildId;
      CreateApplicationVersionRequest cavr = new CreateApplicationVersionRequest().withApplicationName(applicationName).withDescription(applicationName+" build "+buildId)
              .withVersionLabel(appVersionLabel).withSourceBundle(warLocation);
      getLog().info("Creating new elastic beanstalk version "+cavr);
      eb.createApplicationVersion(cavr);

      // Create a new environment for that version



      Collection<ConfigurationOptionSetting> settings = new LinkedList<ConfigurationOptionSetting>(); // TODO: Read from file
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
      String currentLive = findLiveEnvironment(eb,liveServerDomainName);

      getLog().info("Swapping new version live (old live is "+currentLive+")");
      SwapEnvironmentCNAMEsRequest swap = new SwapEnvironmentCNAMEsRequest().withSourceEnvironmentName(currentLive).withDestinationEnvironmentName(environmentName);
      eb.swapEnvironmentCNAMEs(swap);

      if (terminateOldEnviroment) {
          getLog().info("Waiting " + preFlipLiveWaitSeconds + " seconds post live before terminating old environment");
          safeSleep(preFlipLiveWaitSeconds * 1000);

          getLog().info("Terminating old environment");
          TerminateEnvironmentRequest ter = new TerminateEnvironmentRequest().withEnvironmentName(currentLive);
          eb.terminateEnvironment(ter);
      }

      getLog().info("Deploy is now complete");
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

    private String findLiveEnvironment(AWSElasticBeanstalkClient eb,String domainName)
    {
        //DescribeEnvironmentsRequest der = new DescribeEnvironmentsRequest().withEnvironmentNames(domainName);
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        String rval = null;
        for (EnvironmentDescription ed:res.getEnvironments())
        {
            if (ed.getCNAME().equals(domainName))
            {
                rval = ed.getEnvironmentName();
            }
        }
        return rval;
    }

    private String getEnvironmentColor(AWSElasticBeanstalkClient eb,String environmentName)
    {
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        String rval = null;
        for (EnvironmentDescription ed:res.getEnvironments())
        {
            if (ed.getEnvironmentName().equals(environmentName))
            {
                rval = ed.getHealth();
            }
        }
        return rval;
    }


    private String swapWithLiveEnvironment()
    {
        /*
        10ddb1e7185c% more swapWithLiveEnvironment.sh
echo Searching for live env for $1 - will swap for $2
LINE=`$AWS_API_ROOT/elastic-beanstalk-describe-environments| grep $1`
IFS='|' record=( ${LINE} )
TRIMMED=`echo "${record[7]}" | awk '{gsub(/^ +| +$/,"")} {print  $0 }'`
echo Live is $TRIMMED swapping with $2
$AWS_API_ROOT/elastic-beanstalk-swap-environment-cnames -s $TRIMMED -d $2
         */
        return null;
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

}

