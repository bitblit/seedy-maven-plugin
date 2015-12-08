# Usage: Start new environment

* Note: Read "General Usage" first to get your API keys setup correctly *

## Purpose of this tool

To spawn a new Elastic Beanstalk environment with the built WAR file

## Environment Variables Used
*BUILD_NUMBER* : This is used when deploying a new environment to name it.  Typically provided by Jenkins but can be
set using a -D on the command line.
*BUILD_ID* : This is used when deploying a new environment to name it.  Typically provided by Jenkins but can be
set using a -D on the command line.

#### Parameters
| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
|s3Bucket|The name of the S3 bucket to hold the war/zip file|*yes*| |
|s3Prefix|Prefix to prepend to the war/zip file name in S3|*yes*| |
|applicationFile|Path to the war/zip file| *yes*| |
|applicationName|The name of the application in elastic beanstalk| *yes* | |
|poolConfigFile|Full path to the config file for the environment (see Pool Config File below)| *no* | src/main/config/live-config.json |
|solutionStack|Name of the solution stack to deploy| *no* |Tomcat 8 Java 8 on 64bit Amazon Linux 2015.03 v1.4.5|
|prePingSleepSeconds|How long to wait for EB to setup a stack before we start polling for 'Green' status in seconds| *no* | 300 |
|afterGreenSleepSeconds|How long to wait after a stack reports 'Green' before starting integration tests in seconds| *no* | 30 |
|maxWaitSeconds|How long to wait at any polling stage before giving up entirely in seconds| *no* | 420 |
|liveServerDomainName|Name of the live server (typically applicationName.elasticbeanstalk.com)| *yes*| |

#### Example Usage
```xml
<build>
  ...

  <plugins>
    ...

            <plugin>
                <groupId>com.erigir</groupId>
                <artifactId>seedy-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <afterGreenSleepSeconds>30</afterGreenSleepSeconds>
                    <applicationName>myapp</applicationName>
                    <s3Bucket>seedy-uploads</s3Bucket>
                    <s3Prefix>myapp/</s3Prefix>
                    <poolConfigFile>src/main/config/live-config.json</poolConfigFile>
                    <solutionStack>64bit Amazon Linux 2014.03 v1.0.4 running Tomcat 7 Java 7</solutionStack>
                    <prePingSleepSeconds>300</prePingSleepSeconds>
                    <maxWaitSeconds>420</maxWaitSeconds>
                    <liveServerDomainName>myapp.elasticbeanstalk.com</liveServerDomainName>
                    <preFlipLiveWaitSeconds>15</preFlipLiveWaitSeconds>
                    <terminateOldEnviroment>false</terminateOldEnviroment>
                    <applicationFile>${project.build.directory}/${project.build.finalName}.war</applicationFile>

                    <renameMappings>
                        <renameMapping>
                            <src>r.html</src>
                            <dst>r</dst>
                    </renameMappings>
                </configuration>
            </plugin>
  </plugins>
</build>
```

#### Pool Config File
This file allows you to configure the pool of for the elastic beanstalk environment.  There are lots of possible
settings, see AWS docs for details.

Example
```json
[
    {
        "Namespace": "aws:autoscaling:asg",
        "OptionName": "MinSize",
        "Value": "1"
    },
    {
        "Namespace": "aws:autoscaling:asg",
        "OptionName": "MaxSize",
        "Value": "4"
    },
    {
        "Namespace": "aws:autoscaling:launchconfiguration",
        "OptionName": "InstanceType",
        "Value": "t1.micro"
    },
    {
        "Namespace": "aws:autoscaling:launchconfiguration",
        "OptionName": "EC2KeyName",
        "Value": "your-keypair-name"
    },
    {
        "Namespace": "aws:autoscaling:launchconfiguration",
        "OptionName": "IamInstanceProfile",
        "Value": "your-role-name"
    },
    {
        "Namespace": "aws:autoscaling:launchconfiguration",
        "OptionName": "MonitoringInterval",
        "Value": "5 minutes"
    },
    {
        "Namespace": "aws:elasticbeanstalk:sns:topics",
        "OptionName": "Notification Topic ARN",
        "Value": "your-topic-arn"
    },
    {
        "Namespace": "aws:elasticbeanstalk:hostmanager",
        "OptionName": "LogPublicationControl",
        "Value": "true"
    },
    {
        "Namespace": "aws:elb:loadbalancer",
        "OptionName": "CrossZone",
        "Value": "true"
    },
    {
        "Namespace": "aws:elb:policies",
        "OptionName": "ConnectionDrainingEnabled",
        "Value": "true"
    },
    {
        "Namespace": "aws:elb:policies",
        "OptionName": "Stickiness Policy",
        "Value": "true"
    },
    {
        "Namespace": "aws:elb:policies",
        "OptionName": "Stickiness Cookie Expiration",
        "Value": "14400"
    },
    {
        "Namespace": "aws:elasticbeanstalk:application:environment",
        "OptionName": "example env property name",
        "Value": "example env property value"
    },
    {
        "Namespace" : "aws:elb:loadbalancer",
        "OptionName": "SSLCertificateId",
        "Value": "your SSL certificate ARN"
    }
]
```


### flip-environment
Purpose: To swap URL's on an Elastic Beanstalk environment (typically after the integration tests are run on an environment
spawned by "start-new-environment"

Note: This is still under development as of version 0.6

#### Parameters
| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
|preFlipLiveWaitSeconds|How long to wait after successful integration test before flipping to live (in seconds)| *no* | 15 |
|terminateOldEnviroment|Whether to delete the old 'live' environment after flipping new system live| *no* | false |


