# Usage: S3 Upload

* Note: Read "General Usage" first to get your API keys setup correctly *

## Purpose of this tool

To perform a series of transforms on a local directory, and then upload it to an S3 bucket

#### Parameters

| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
| doNotUpload| If true, files are processed but not sent to S3 | *no* | false |
| source | The file/folder to upload | *yes* | |
| s3Bucket | The s3 bucket to upload to | *yes* | |
| s3Prefix | Prefix to prepend on all uploaded files | *no* | |
| endpoint | Force override of S3 endpoint (typically for different regions) | *no* | |
| recursive | If a directory, recursively upload subdirectories | *no* | false | 
| deleteNonMatch | If true, any files in the S3 bucket not matching a local file are deleted (skips backup directory) | *no* | false | 
| deltaMethod | Checks whether the remote file is already the same and if so, doesnt upload.  Options are MD5, DATE, NONE | *no* | MD5 | 
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
| replacement | A single replacement definition | *no* | |
| md5 | A single md5 definition | *no* | Regext matches everything by default |
| htmlCompression | A single html compression definition | *no* | |
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

##### Replacement
Runs the replacement processor.  A lot like the includes above, except that the replacement text comes
 from the tag instead of other files.  Maps from a Pattern->String, any content between prefix and suffix
 that matches the pattern is replaced with the right hand side.  Anything that doesn't match is replaced with
 blank strings.

Example
```xml
<replacement>
    <includeRegex>.*\.html</includeRegex>
    <prefix><![CDATA[<!--SI:]]></prefix>
    <suffix><![CDATA[:SI-->]]></suffix>
    <replace>
        <a>b</a>
        <b>c</b>
    </replace>
</replacement>
```

##### MD5
Define the set of files that should have MD5 run on them (the hex encoding of it is put into a amz-metadata, which will
match the etag.

Example
```xml
<md5>
    <includeRegex>.*</includeRegex>
</md5>

##### HtmlCompression
Define the set of files that should have HTML compression applied to them.  This removes all
comments and condenses any multiple spaces between tags to 1 (more configurability later).

Example
```xml
<htmlCompression>
    <includeRegex>.*\\.html</includeRegex>
</htmlCompression>
