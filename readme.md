seedy-maven-plugin
======================
Helper tools to implement Continuous Deployment atop AWS Services (especially with Jenkins)

Acknowledgements
----------------
This tool got its start from https://github.com/bazaarvoice/s3-upload-maven-plugin to which it is
heavily indebted, especially the S3 uploader.  While I have decided to go ahead and fork from there 
to allow more active development (last commit was 12/31/2013) and add more HTTP-centric things to the
tool, this is in no way intended as disrespect to what is still an awesome tool.


Configuration parameters
------------------------

| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
|s3Bucket|The name of the S3 bucket to hold the war/zip file|*yes*| |
|s3Prefix|Prefix to prepend to the war/zip file name in S3|*yes*| |
|applicationFile|Path to the war/zip file| *yes*| |
|applicationName|The name of the application in elastic beanstalk| *yes* | |
|liveServerDomainName|Name of the live server (typically applicationName.elasticbeanstalk.com)| *yes*| |
|poolConfigFile|Full path to the config file for the environment| *no* | src/main/config/live-config.json |
|solutionStack|Name of the solution stack to deploy| *no* |64bit Amazon Linux 2014.03 v1.0.4 running Tomcat 7 Java 7|
|prePingSleepSeconds|How long to wait for EB to setup a stack before we start polling for 'Green' status in seconds| *no* | 300 |
|afterGreenSleepSeconds|How long to wait after a stack reports 'Green' before starting integration tests in seconds| *no* | 30 |
|maxWaitSeconds|How long to wait at any polling stage before giving up entirely in seconds| *no* | 420 |
|preFlipLiveWaitSeconds|How long to wait after successful integration test before flipping to live (in seconds)| *no* | 15 |
|terminateOldEnviroment|Whether to delete the old 'live' environment after flipping new system live| *no* | false |

General Flow
------------


Pool Config File
----------------


IAM Configuration For Seedy User/Role
-------------------------------------


Jenkins Integration
-------------------


Example Usage
----------------------
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
                </configuration>
            </plugin>
  </plugins>
</build>
```

FAQ
---

* Why do I have to set environmental variables for my Key/Secret instead of using config parameters?

Because you probably check your POM file into source control, and checking your keys into source control
is a really bad idea (TM), so I'd like to save you from doing that.  Of course, you could still put them
in using some Maven tricks, but there is only so far I can go to stop you from shooting yourself in the
foot, metaphorically (or physically, for that matter).

* How do I exclude a file from upload in the s3-upload plugin?

Right now you can't - the configs set what metadata to set on the uploaded files, not whether or not to upload them
