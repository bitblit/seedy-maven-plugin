# seedy-maven-plugin

Helper tools to implement Continuous Deployment atop AWS Services (especially with Jenkins)

## Acknowledgements
This tool got its start from https://github.com/bazaarvoice/s3-upload-maven-plugin to which it is
heavily indebted, especially the S3 uploader.  While I have decided to go ahead and fork from there 
to allow more active development (last commit was 12/31/2013) and add more HTTP-centric things to the
tool, this is in no way intended as disrespect to what is still an awesome tool.

## General Notes
Seedy ALWAYS uses the DefaultAWSCredentialProvider, and doesn't let you set a key/secret via properties.  Why?
Because you probably check your POM file into source control, and checking your keys into source control
is a really bad idea (TM), so I'd like to save you from doing that.  Of course, you could still put them
in using some Maven tricks, but there is only so far I can go to stop you from shooting yourself in the
foot, metaphorically (or physically, for that matter).

## IAM Configuration For Seedy User/Role
See seedy-iam-permissions.json

## Release Notes

### Version 0.6
This is a backwards compatible feature release
* Extracted file processing from S3 Upload into its own library so it can be used standalone (Drigo, in wrench-drigo)
* Added ability to run Babel processing prior to s3 upload (mainly for JSX processing)
* Added ability to exclude files from upload
* Added dynamic include capability (parse files and replace tags with contents of other files, like server side includes)
* Finally started to fix this documentation


## Common Configuration Parameters
These parameters are used by all the targets, since they define how your machine interacts with AWS
*assumedRoleArn* : ARN of an AWS role to assume for the deployment.  Typically used when the 
build server running seedy belongs to a different AWS account than the one hosting the deployment.
*assumedRoleExternalId*: External IS of an AWS role to assume for the deployment.  Typically used when the 
build server running seedy belongs to a different AWS account than the one hosting the deployment.

## Environment Variables Used
*BUILD_NUMBER* : This is used when deploying a new environment to name it.  Typically provided by Jenkins but can be
set using a -D on the command line.
*BUILD_ID* : This is used when deploying a new environment to name it.  Typically provided by Jenkins but can be
set using a -D on the command line.

## Targets

### s3-upload
Purpose: To perform a series of transforms on a local directory, and then upload it to an S3 bucket

#### Parameters

| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
| doNotUpload| If true, files are processed but not sent to S3 | *no* | false |
| source | The file/folder to upload | *yes* | |
| s3Bucket | The s3 bucket to upload to | *yes* | |
| s3Prefix | Prefix to prepend on all uploaded files | *no* | |
| endpoint | Force override of S3 endpoint (typically for different regions) | *no* | |
| recursive | If a directory, recursively upload subdirectories | *no* | false | 
| backupCurrent| If true, the current contents of the S3 bucket are backed up prior to upload | *no* | true |
| backupPrefix | A folder of the format *backupPrefix-yyyy-mm-dd-hh-mm-ss* will be created to contain the backup | *no* | __seedy_backup_ | 
| objectMetadataSettings | List of objectMetadataSettings definitions | *no* | |
| htmlResourceBatching | List of htmlResourceBatching definitions | *no* | |
| fileCompression | A single file compression definition | *no* | | 
| cssCompilation | A single css compilation definition | *no* | |
| babelCompilation | A single babel compilation definition | *no* | |
| javascriptCompilation | A single javascript compilation definition | *no* | |
| validators | A list of validation setting definitions | *no* | |
| exclusions | A list of exclusion definitions | *no* | |
| processIncludes | A list of process includes definitions | *no* | |
| renameMappings | A list of rename mapping definitions | *no* | | 

#### Definitions

##### ObjectMetadataSetting
Used to set object metadata on upload files

Sample
```xml
<objectMetadataSettings>
    <objectMetadataSetting>
        <includeRegex>.*</includeRegex>
        <cacheControl>max-age=30</cacheControl>
        <contentType>text/html; charset=utf-8</contentType>
        <contentDisposition>attachment</contentType>
        <contentEncoding>gzip</contentType>
        <userMetaData>
            <uploadTime>${maven.build.timestamp}</uploadTime>
        </userMetaData>
    </objectMetadataSetting>
</objectMetadataSettings>
```

##### HtmlResourceBatching
Performs 2 actions - first, it finds a set of files that match the criteria, and combines them all into a single
file.  Secondly, it parses all the html files in the folder, and if it finds a special comment flag pair, it replaces
whats between them with a pointer to the new file.  Used to combine large quantities of JS or CSS files into a single
download

Sample
```xml
  <htmlResourceBatchings>
    <htmlResourceBatching>
        <flagName>INDEXSIGBUNDLE</flagName>
        <includeRegex>.*/js/index-sig-bundle/.*\.js</includeRegex>
        <replaceInHtmlRegex>.*\.html</replaceInHtmlRegex>
        <wrapper>JAVASCRIPT</wrapper>
        <outputFileName>js/index-sig-bundle.js</outputFileName>
        <replaceText>/js/index-sig-bundle.js</replaceText>
        <deleteSource>false</deleteSource>
    </htmlResourceBatching>
  </htmlResourceBatching>
```

Example HTML replacement

```xml
<!--INDEXSIGBUNDLE-->
<script src="/js/index-sig-bundle/bootbox.min.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/form2js.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/form-validation.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/gmap-helper.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/id-number-support.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/jquery.blockUI.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/test-harness.js" type="text/javascript"></script>
<script src="/js/index-sig-bundle/custom.js" type="text/javascript"></script>
<!--END:INDEXSIGBUNDLE-->
```

##### FileCompression
Define the set of files that should have GZIP compression applied to them (note, automatically sets
the content-encoding header on those files to 'gzip' as a side effect.

Example
```xml
<fileCompression>
    <includeRegex>.*</includeRegex>
</fileCompression>
```

##### CssCompilation
Uses the YUI css compiler on anything matching

Example
```xml
<cssCompilation>
    <includeRegex>.*</includeRegex>
</cssCompilation>
```

##### BabelCompilation
Uses the Babel JS compiler on anything matching

Example
```xml
<babelCompilation>
    <includeRegex>.*\.jsx</includeRegex>
</babelCompilation>
```

##### JavascriptCompilation
Uses the Closure javascript compiler on anything matching.  Options for mode are:
CLOSURE_WHITESPACE, CLOSURE_BASIC, CLOSURE_ADVANCED

Example
```xml
<javascriptCompilation>
    <includeRegex>.*\.js</includeRegex>
    <mode>CLOSURE_WHITESPACE</mode>
</javascriptCompilation>
```

##### Validators
Runs validation on the matching files, throws an error if they fail and prevents upload.  Options are
JSON, XML

Example
```xml
<validators>
    <validator>
        <type>JSON</type>
        <includeRegex>.*\.json</includeRegex>
    </validator>
</validators>
```
##### Exclusions
Excludes matching files from processing and uploading

Example
```xml
<exclusions>
    <exclusion>
        <includeRegex>.*WEB-INF.*</includeRegex>
    </exclusion>
</exclusions>
```

##### ProcessIncludes
Runs the includes processor (vaguely like server side includes).  Include source
is always the root (same as source, above)

Example
```xml
<processIncludes>
    <processInclude>
        <includeRegex>.*\.html</includeRegex>
        <prefix><![CDATA[<!--SI:]]></prefix>
        <suffix><![CDATA[:SI-->]]></suffix>
    </processInclude>
</processIncludes>
```

##### RenameMappings
Renames any matching files to the new name

Example
```xml
<renameMappings>
    <renameMapping>
        <src>r.html</src>
        <dst>r</dst>
    </renameMapping>
</renameMappings>
```

### start-new-environment
Purpose: To spawn a new Elastic Beanstalk environment with the built WAR file

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


