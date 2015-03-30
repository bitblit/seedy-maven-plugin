package com.erigir.maven.plugin;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.erigir.maven.plugin.processor.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 Copyright 2014 Christopher Weiss

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

@Mojo(name = "s3-upload")
public class S3UploadMojo extends AbstractSeedyMojo implements ObjectMetadataProvider {

    /**
     * If deploying to a different account, this is the ARN of the role in that account
     * with privs to execute the deployment
     */
    @Parameter(property = "s3-upload.assumedRoleArn")
    String assumedRoleArn;

    /**
     * If deploying to a different account, this is the external ID set on the role in that account
     * with privs to execute the deployment
     */
    @Parameter(property = "s3-upload.assumedRoleExternalId")
    String assumedRoleExternalId;

    /**
     * Execute all steps up except the upload to the S3.
     * This can be set to true to perform a "dryRun" execution.
     */
    @Parameter(property = "s3-upload.doNotUpload", defaultValue = "false")
    boolean doNotUpload;

    /**
     * The file/folder to upload.
     */
    @Parameter(property = "s3-upload.source", required = true)
    String source;

    /**
     * The bucket to upload into.
     */
    @Parameter(property = "s3-upload.s3Bucket", required = true)
    String s3Bucket;

    /**
     * The file/folder (in the bucket) to create.
     * If this is not present or empty, will upload to the root of the bucket
     */
    @Parameter(property = "s3-upload.s3Prefix")
    String s3Prefix;

    /**
     * Force override of endpoint for S3 regions such as EU.
     */
    @Parameter(property = "s3-upload.endpoint")
    String endpoint;

    /**
     * In the case of a directory upload, recursively upload the contents.
     */
    @Parameter(property = "s3-upload.recursive", defaultValue = "false")
    boolean recursive;

    /**
     * List of object metadata settings
     */
    @Parameter(property = "s3-upload.objectMetadataSettings")
    List<ObjectMetadataSetting> objectMetadataSettings;

    /**
     * List of html resource batchers
     */
    @Parameter(property = "s3-upload.htmlResourceBatching")
    List<HtmlResourceBatching> htmlResourceBatchings;

    /**
     */
    @Parameter(property = "s3-upload.fileCompression")
    FileCompression fileCompression;

    /**
     */
    @Parameter(property = "s3-upload.cssCompilation")
    CssCompilation cssCompilation;

    /**
     */
    @Parameter(property = "s3-upload.javascriptCompilation")
    JavascriptCompilation javascriptCompilation;

    /**
     */
    @Parameter(property = "s3-upload.validators")
    List<ValidationSetting> validators;

    /**
     * Whether to move all the current values into a subdirectory
     */
    @Parameter(property = "s3-upload.backupCurrent", defaultValue = "true")
    boolean backupCurrent;

    /**
     * If a backup is performed, the subdirectory will be {backupPrefix}-yyyy-mm-dd-hh-mm-ss
     */
    @Parameter(property = "s3-upload.backupPrefix", defaultValue = "__seedy_backup_")
    String backupPrefix;

    /**
     * If specified, files named here get mapped to target
     */
    @Parameter(property = "s3-upload.renameMappings")
    List<RenameMapping> renameMappings;


    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (objectMetadataSettings == null) {
                getLog().info("No upload configs specified, using default");
                objectMetadataSettings = new LinkedList<>();
            }

            if (fileCompression != null && fileCompression.getIncludeRegex() != null) {
                getLog().info("File compression set, adding metadata setting");
                ObjectMetadataSetting oms = new ObjectMetadataSetting();
                oms.setContentEncoding("gzip");
                oms.setIncludeRegex(fileCompression.getIncludeRegex());

                List<ObjectMetadataSetting> omsNew = new LinkedList<>();
                omsNew.addAll(objectMetadataSettings);
                omsNew.add(oms);
                objectMetadataSettings = omsNew;
            }

            File sourceFile = new File(source);
            if (!sourceFile.exists()) {
                throw new MojoExecutionException("File/folder doesn't exist: " + source);
            }

            AmazonS3 s3 = s3();


            // This is designed to be easy to understand, not particularly efficient.  We'll
            // work on efficiency later

            // We create a copy of the file/directory because we are going to use transfer manager to post it all
            // and we assume network is a tighter bound than disk space.  Yup, tradeoff time
            // First pass - copy all the files

            File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
            File myTemp = new File(sysTempDir, UUID.randomUUID().toString());
            myTemp.deleteOnExit(); // clean up after ourselves
            // Copy all files over
            FileProcessorUtils.copyFolder(sourceFile, myTemp);

            getLog().info("Checking rename mappings");
            if (renameMappings!=null)
            {
                for (RenameMapping r:renameMappings)
                {
                    File input = new File(myTemp, r.getSrc());
                    if (input.exists())
                    {
                        File output = new File(myTemp, r.getDst());
                        getLog().info("Renaming "+input+" to "+output);
                        input.renameTo(output);
                    }
                    else
                    {
                        getLog().info("Rename Mapping "+input+" doesnt exist, skipping");
                    }
                }

            }

            // Now, run the configured file validators
            getLog().info("Running validators");
            if (validators != null && validators.size() > 0) {

                for (ValidationSetting validator : validators) {
                    ValidationProcessor proc = new ValidationProcessor(validator.getType());
                    applyProcessorToFileList(findMatchingFiles(myTemp, Pattern.compile(validator.getIncludeRegex())), proc);
                }
            }

            // Now, do any batching
            getLog().info("Doing HTML resource batching");
            if (htmlResourceBatchings != null) {
                for (HtmlResourceBatching h : htmlResourceBatchings) {
                    List<File> matching = new LinkedList<>();
                    findMatchingFiles(myTemp, Pattern.compile(h.getIncludeRegex()), matching);

                    if (matching.size() > 0) {
                        File toOutput = new File(myTemp, h.getOutputFileName());
                        getLog().info("Creating output file : " + toOutput);
                        h.combine(matching, toOutput);

                        List<File> htmlToFilter = new LinkedList<>();
                        if (h.getReplaceInHtmlRegex() != null) {
                            ApplyFilterProcessor ap = new ApplyFilterProcessor(h);
                            applyProcessorToFileList(findMatchingFiles(myTemp, Pattern.compile(h.getReplaceInHtmlRegex())), ap);
                        } else {
                            getLog().info("Not performing html replacement");
                        }
                    } else {
                        getLog().info("HTMLBatcher didn't find any files matching : " + h.getIncludeRegex() + ", skipping");
                    }
                }
            }

            // Now, apply Css compression if applicable
            getLog().info("Checking CSS compression");
            if (cssCompilation != null && cssCompilation.getIncludeRegex() != null) {
                YUICompileContentModelProcessor proc = new YUICompileContentModelProcessor();

                applyProcessorToFileList(findMatchingFiles(myTemp, Pattern.compile(cssCompilation.getIncludeRegex())), proc);
            }

            getLog().info("Checking JS compression");
            if (javascriptCompilation != null && javascriptCompilation.getIncludeRegex() != null) {
                JavascriptCompilerFileProcessor ipcc = new JavascriptCompilerFileProcessor();
                ipcc.setMode(javascriptCompilation.getMode());
                try {
                    applyProcessorToFileList(findMatchingFiles(myTemp, Pattern.compile(javascriptCompilation.getIncludeRegex())), ipcc);
                } catch (Throwable t) {
                    getLog().error("Caught " + t);
                    throw t;
                }
            }

            getLog().info("Checking GZIP compression");
            if (fileCompression != null && fileCompression.getIncludeRegex() != null) {
                GZipFileProcessor gzfp = new GZipFileProcessor();
                applyProcessorToFileList(findMatchingFiles(myTemp, Pattern.compile(fileCompression.getIncludeRegex())), gzfp);
                getLog().info("GZIP compression saved " + GZipFileProcessor.totalSaved + " bytes in total");
            }

            if (doNotUpload) {
                getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run)",
                        myTemp, s3Bucket, s3Prefix));

                return;
            }

            if (backupCurrent)
            {
                String backupSubdir = backupPrefix+new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-zzz").format(new Date());
                getLog().info("Backup specified, using subdirectory "+backupSubdir);
                copyAllToBackup(s3, backupSubdir);
            }

            getLog().info("About to being upload of files");
            boolean success = upload(s3, myTemp);
            if (!success) {
                throw new MojoExecutionException("Unable to upload file to S3.");
            }

            getLog().info(String.format("File %s uploaded to s3://%s/%s",
                    sourceFile, s3Bucket, s3Prefix));
        }
        finally {
            getLog().info("Seedy: All processing finished.");
        }
    }

    private void copyAllToBackup(AmazonS3 s3, String backupSubdir) throws MojoExecutionException
    {
        getLog().info("Backing up contents of "+s3Bucket+"/"+s3Prefix+" to "+backupSubdir);
        String prefix = (s3Prefix==null)?"":s3Prefix;


        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().
                withBucketName(s3Bucket)
                .withPrefix(prefix));

        for (int i = 0; i < objectListing.getObjectSummaries().size(); i++) {
            S3ObjectSummary os = objectListing.getObjectSummaries().get(i);
            // Strip the folder itself
            if (os.getKey().length() > prefix.length()) {
                String subSect = os.getKey().substring(prefix.length());
                if (subSect.startsWith(backupPrefix))
                {
                    getLog().info("Skipping "+subSect+" its a previous backup directory");
                }
                else
                {
                    String newFile = prefix+backupSubdir+"/"+subSect;
                    getLog().info("Copying " + os.getKey() + " to " + newFile);
                    s3.copyObject(s3Bucket,os.getKey(),s3Bucket,newFile);
                }
            }
        }

    }

    private boolean upload(AmazonS3 s3, File sourceFile) throws MojoExecutionException {
        TransferManager mgr = new TransferManager(s3);

        Transfer transfer;
        if (sourceFile.isFile()) {
            transfer = mgr.uploadFileList(s3Bucket, s3Prefix, sourceFile.getParentFile(), Arrays.asList(sourceFile), this);
        } else if (sourceFile.isDirectory()) {
            transfer = mgr.uploadDirectory(s3Bucket, s3Prefix, sourceFile, recursive, this);

        } else {
            throw new MojoExecutionException("File is neither a regular file nor a directory " + sourceFile);
        }
        try {
            getLog().info(String.format("About to transfer %s bytes...", transfer.getProgress().getTotalBytesToTransfer()));
            transfer.waitForCompletion();
            getLog().info(String.format("Completed transferring %s bytes...", transfer.getProgress().getBytesTransferred()));
        } catch (InterruptedException e) {
            return false;
        } catch (AmazonS3Exception as3e) {
            throw new MojoExecutionException("Error uploading to S3", as3e);
        }

        return true;
    }

    @Override
    public void provideObjectMetadata(File file, ObjectMetadata objectMetadata) {
        getLog().debug(String.format("Creating metadata for %s (size=%s)", file.getName(), file.length()));

        for (ObjectMetadataSetting e : objectMetadataSettings) {
            if (e.shouldInclude(file,getLog())) {
                getLog().info("Applying config " + e);
                e.update(objectMetadata);
            }
        }
    }

    public void applyProcessorToFileList(List<File> src, FileProcessor processor)
            throws MojoExecutionException
    {
        assert(src!=null && processor!=null);

        for (File f:src)
        {
            getLog().info("Applying "+processor.getClass().getName()+" to "+src);
            processor.process(getLog(),f);
        }
    }

    public List<File> findMatchingFiles(File src, Pattern pattern)
    {
        List<File> rval = new LinkedList<>();
        findMatchingFiles(src, pattern, rval);
        getLog().info("Found "+rval.size()+" files matching pattern "+pattern+" : "+rval);
        return rval;
    }

    public void findMatchingFiles(File src, Pattern pattern, List<File> matching)
    {
        assert(src!=null && matching!=null);
        if (src.isFile())
        {
            if (pattern==null || pattern.matcher(src.getAbsolutePath()).matches())
            {
                //getLog().info("Matching " + pattern + " to " + src);
                matching.add(src);
            }
        }
        else
        {
            for (String s:src.list())
            {
                findMatchingFiles(new File(src, s), pattern, matching);
            }
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

}
