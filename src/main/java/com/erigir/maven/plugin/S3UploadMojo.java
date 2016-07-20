package com.erigir.maven.plugin;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.erigir.maven.plugin.s3uploadparam.*;
import com.erigir.wrench.drigo.*;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

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
     * If the file doesn't exist locally but does exist on S3, delete it
     * (excludes any files with the backupPrefix)
     */
    @Parameter(property = "s3-upload.deleteNonMatch", defaultValue = "false")
    boolean deleteNonMatch;

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
    List<ObjectMetadataSettingParam> objectMetadataSettings;

    /**
     * List of html resource batchers
     */
    @Parameter(property = "s3-upload.htmlResourceBatching")
    List<HtmlResourceBatchingParam> htmlResourceBatchings;

    /**
     */
    @Parameter(property = "s3-upload.fileCompression")
    FileCompressionParam fileCompression;

    /**
     */
    @Parameter(property = "s3-upload.md5")
    MD5CalculationParam md5;

    /**
     */
    @Parameter(property = "s3-upload.cssCompilation")
    CssCompilationParam cssCompilation;

    /**
     */
    @Parameter(property = "s3-upload.htmlCompression")
    HtmlCompressionParam htmlCompression;

    /**
     */
    @Parameter(property = "s3-upload.replacement")
    ProcessReplaceParam replacement;

    /**
     */
    @Parameter(property = "s3-upload.babelCompilation")
    BabelCompilationParam babelCompilation;

    /**
     */
    @Parameter(property = "s3-upload.javascriptCompilation")
    JavascriptCompilationParam javascriptCompilation;

    /**
     */
    @Parameter(property = "s3-upload.validators")
    List<ValidationSettingParam> validators;

    /**
     */
    @Parameter(property = "s3-upload.exclusions")
    List<ExclusionParam> exclusions;

    /**
     */
    @Parameter(property = "s3-upload.processIncludes")
    List<ProcessIncludesParam> processIncludes;

    /**
     * Whether to move all the current values into a subdirectory
     */
    @Parameter(property = "s3-upload.backupCurrent", defaultValue = "true")
    boolean backupCurrent;

    /**
     * Whether to move all the current values into a subdirectory
     */
    @Parameter(property = "s3-upload.deltaMethod", defaultValue = "MD5")
    DeltaCalculationMethod deltaMethod;


    /**
     * If a backup is performed, the subdirectory will be {backupPrefix}-yyyy-mm-dd-hh-mm-ss
     */
    @Parameter(property = "s3-upload.backupPrefix", defaultValue = "__seedy_backup_")
    String backupPrefix;

    /**
     * If specified, files named here get mapped to target
     */
    @Parameter(property = "s3-upload.renameMappings")
    List<RenameMappingParam> renameMappings;

    private DrigoResults drigoResults;

    @Override
    public void execute() throws MojoFailureException {
        try {
            // Setup the source directory
            File sourceFile = new File(source);
            if (!sourceFile.exists()) {
                throw new MojoFailureException("File/folder doesn't exist: " + source);
            }

            // Setup the temporary directory
            File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
            File myTemp = new File(sysTempDir, UUID.randomUUID().toString());
            myTemp.deleteOnExit(); // clean up after ourselves
            getLog().info("Seedy using Drigo temp directory : " + myTemp.getAbsolutePath());


            // Build the drigo configuration
            DrigoConfiguration conf = new DrigoConfiguration();
            conf.setSrc(sourceFile);
            conf.setDst(myTemp);

            if (objectMetadataSettings != null) {
                getLog().info("ObjectMetadataSettings found, adding to Drigo configuration");
                List<AddMetadata> addMetadata = new LinkedList<>();
                for (ObjectMetadataSettingParam oms : objectMetadataSettings) {
                    addMetadata.addAll(oms.toAddMetadata());
                }
                conf.setAddMetadata(addMetadata);
            }

            if (babelCompilation != null && babelCompilation.getIncludeRegex()!=null) {
                getLog().info("Babel found, adding to Drigo configuration");
                conf.setBabelCompilationIncludeRegex(Pattern.compile(babelCompilation.getIncludeRegex()));
            }

            if (cssCompilation != null && cssCompilation.getIncludeRegex()!=null) {
                getLog().info("CSS compilation found, adding to Drigo configuration");
                conf.setCssCompilationIncludeRegex(Pattern.compile(cssCompilation.getIncludeRegex()));
            }

            if (htmlResourceBatchings != null) {
                getLog().info("HTML resource batching found, adding to Drigo configuration");
                List<HtmlResourceBatching> list = new LinkedList<>();
                for (HtmlResourceBatchingParam p : htmlResourceBatchings) {
                    if (p.getIncludeRegex()!=null)
                    {
                        list.add(p.toDrigoBatching());
                    }
                }
                conf.setHtmlResourceBatching(list);
            }

            if (processIncludes != null) {
                getLog().info("Process Includes found (" + processIncludes.size() + ") adding to Drigo configuration");
                List<ProcessIncludes> list = new LinkedList<>();
                for (ProcessIncludesParam p : processIncludes) {
                    if (p.getIncludeRegex()!=null)
                    {
                        getLog().info("PI:" + p.getIncludeRegex() + " p:" + p.getPrefix() + " s:" + p.getSuffix());
                        list.add(p.toDrigo());
                    }
                }
                conf.setProcessIncludes(list);
            }

            if (replacement != null) {
                if (replacement.getIncludeRegex()!=null)
                {
                    getLog().info("Replacement found (" + replacement.getReplace().size() + " mappings) adding to Drigo configuration");
                    conf.setProcessReplace(replacement.toDrigo());
                }
            }

            if (exclusions != null) {
                getLog().info("Exclusions found (" + exclusions.size() + ") adding to Drigo configuration");
                List<Exclusion> list = new LinkedList<>();
                for (ExclusionParam p : exclusions) {
                    if (p.getIncludeRegex()!=null)
                    {
                        list.add(p.toDrigo());
                    }
                }
                conf.setExclusions(list);
            }

            if (fileCompression != null && fileCompression.getIncludeRegex()!=null) {
                getLog().info("File Compression found, adding to Drigo configuration");
                conf.setFileCompressionIncludeRegex(Pattern.compile(fileCompression.getIncludeRegex()));
            }

            if (htmlCompression != null && htmlCompression.getIncludeRegex()!=null) {
                getLog().info("HTML Compression found, adding to Drigo configuration");
                conf.setHtmlCompression(Pattern.compile(htmlCompression.getIncludeRegex()));
            }

            if (javascriptCompilation != null && javascriptCompilation.getIncludeRegex()!=null) {
                getLog().info("Javascript Compilation found, adding to Drigo configuration");
                conf.setJavascriptCompilation(javascriptCompilation.toDrigo());
            }

            if (validators != null) {
                getLog().info("Validators found, adding to Drigo configuration");
                List<ValidationSetting> list = new LinkedList<>();
                for (ValidationSettingParam p : validators) {
                    if (p.getIncludeRegex()!=null)
                    {
                        list.add(p.toDrigo());
                    }
                }
                conf.setValidation(list);
            }

            if (renameMappings != null) {
                getLog().info("Rename mappings found (" + renameMappings.size() + ") adding to Drigo configuration");
                List<RenameMapping> list = new LinkedList<>();
                for (RenameMappingParam p : renameMappings) {
                    list.add(p.toDrigo());
                }
                conf.setRenameMappings(list);
            }

            if (md5 != null && md5.getIncludeRegex()!=null) {
                getLog().info("MD5 requested, adding to Drigo configuration");
                conf.setMd5GenerationIncludeRegex(Pattern.compile(md5.getIncludeRegex()));
            }

            Drigo processor = new Drigo();
            drigoResults = processor.execute(conf);

            AmazonS3 s3 = s3();
            processDeltas(s3, drigoResults);

            if (doNotUpload) {
                getLog().info("Processing finished, doNotUpload specified.");
                getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run)",
                        myTemp, s3Bucket, s3Prefix));

                return;
            } else {

                if (backupCurrent) {
                    String backupSubdir = backupPrefix + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-zzz").format(new Date());
                    getLog().info("Backup specified, using subdirectory " + backupSubdir);
                    copyAllToBackup(s3, backupSubdir);

                }
                getLog().info("About to being upload of files");

                boolean success = upload(s3, myTemp);
                if (!success) {
                    throw new MojoFailureException("Unable to upload file to S3.");
                }

                getLog().info(String.format("File %s uploaded to s3://%s/%s",
                        sourceFile, s3Bucket, s3Prefix));

            }

        } finally {
            getLog().info("Seedy: All processing finished.");
        }
    }

    private void processDeltas(AmazonS3 s3, DrigoResults results) {
        getLog().info("Processing deltas on results of size " + results.getMetadata().size() + " type " + deltaMethod + " delete is " + deleteNonMatch);
        List<S3ObjectSummary> noMatchLocal = new LinkedList<>();


        if (deltaMethod != DeltaCalculationMethod.NONE || deleteNonMatch) {
            long start = System.currentTimeMillis();
            List<String> keysToDelete = new LinkedList<>();

            String prefix = (s3Prefix == null) ? "" : s3Prefix;
            ObjectListing listing = s3.listObjects(s3Bucket, prefix);


            int sameCount = 0;
            long sameBytes = 0;
            boolean shouldContinue = true;

            while (shouldContinue) {


                for (S3ObjectSummary sos : listing.getObjectSummaries()) {
                    getLog().info("Checking delta on " + sos.getKey());
                    String s3Path = sos.getKey();
                    File localFile = new File(results.getSourceConfiguration().getDst(), s3Path);
                    if (localFile.exists()) {
                        Map<String, String> local = results.getMetadata().get(localFile);
                        switch (deltaMethod) {
                            case DATE:
                                long modifiedRemote = sos.getLastModified().getTime();
                                long modifiedLocal = localFile.lastModified(); // TODO: no way this works, this file was just made
                                if (modifiedRemote <= modifiedLocal) {
                                    getLog().debug("Remote file is newer, skipping");
                                    sameCount++;
                                    sameBytes += localFile.length();
                                    if (!localFile.delete()) {
                                        getLog().warn("Error removing file " + localFile.getName() + " from upload");
                                    }
                                    while (localFile.getParentFile().list().length == 0) {
                                        getLog().info("Parent dir empty, removing it too " + localFile.getParentFile().getName());
                                        localFile = localFile.getParentFile();
                                        localFile.delete();
                                    }
                                } else {
                                    getLog().debug("Retained " + localFile.getName() + " for copy");
                                }
                                break;
                            case MD5:
                                String remoteMD5 = sos.getETag();
                                String localMD5 = local.get("md5-hex");
                                if (remoteMD5 != null && remoteMD5.equals(localMD5)) {
                                    getLog().debug("Content MD5 equal, skipping");
                                    sameCount++;
                                    sameBytes += localFile.length();
                                    if (!localFile.delete()) {
                                        getLog().warn("Error removing file " + localFile.getName() + " from upload");
                                    }
                                    while (localFile.getParentFile().list().length == 0) {
                                        getLog().info("Parent dir empty, removing it too " + localFile.getParentFile().getName());
                                        localFile = localFile.getParentFile();
                                        localFile.delete();
                                    }
                                } else {
                                    getLog().debug("Retained " + localFile.getName() + " for copy");
                                }
                                break;
                            default:
                                getLog().debug("Delta mode none - not processing");
                                break;
                        }
                    } else {
                        getLog().debug("Remote file " + sos.getKey() + " doesnt exist locally");
                        if (deleteNonMatch) {
                            keysToDelete.add(sos.getKey());
                        }
                    }

                }

                shouldContinue = listing.isTruncated();
                if (shouldContinue) {
                    getLog().debug("Pulling another batch from S3");
                    listing = s3.listNextBatchOfObjects(listing);
                }

            }
            getLog().info("Delta check removed " + sameCount + " files (out of " + drigoResults.getMetadata().size() + ") - saving " + sameBytes + " bytes transfer");

            if (deleteNonMatch) {
                getLog().info("Found " + keysToDelete.size() + " files to delete from server (" + keysToDelete + ")");
                for (String key : keysToDelete) {
                    getLog().info("Deleting file : " + key);
                    s3.deleteObject(s3Bucket, key);
                }
            }
            getLog().info("Delta check took " + (System.currentTimeMillis() - start) + " ms to run");

        } else {
            getLog().info("Skipping, NONE specified and delete is false");
        }


    }


    private void copyAllToBackup(AmazonS3 s3, String backupSubdir) throws MojoFailureException {
        getLog().info("Backing up contents of " + s3Bucket + "/" + s3Prefix + " to " + backupSubdir);
        String prefix = (s3Prefix == null) ? "" : s3Prefix;


        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().
                withBucketName(s3Bucket)
                .withPrefix(prefix));

        for (int i = 0; i < objectListing.getObjectSummaries().size(); i++) {
            S3ObjectSummary os = objectListing.getObjectSummaries().get(i);
            // Strip the folder itself
            if (os.getKey().length() > prefix.length()) {
                String subSect = os.getKey().substring(prefix.length());
                if (subSect.startsWith(backupPrefix)) {
                    getLog().info("Skipping " + subSect + " its a previous backup directory");
                } else {
                    String newFile = prefix + backupSubdir + "/" + subSect;
                    getLog().info("Copying " + os.getKey() + " to " + newFile);
                    s3.copyObject(s3Bucket, os.getKey(), s3Bucket, newFile);
                }
            }
        }

    }

    private boolean upload(AmazonS3 s3, File sourceFile) throws MojoFailureException {
        TransferManager mgr = new TransferManager(s3);

        Transfer transfer;
        if (sourceFile.isFile()) {
            transfer = mgr.uploadFileList(s3Bucket, s3Prefix, sourceFile.getParentFile(), Arrays.asList(sourceFile), this);
        } else if (sourceFile.isDirectory()) {
            transfer = mgr.uploadDirectory(s3Bucket, s3Prefix, sourceFile, recursive, this);

        } else {
            throw new MojoFailureException("File is neither a regular file nor a directory " + sourceFile);
        }
        try {
            getLog().info(String.format("About to transfer %s bytes...", transfer.getProgress().getTotalBytesToTransfer()));
            transfer.waitForCompletion();
            getLog().info(String.format("Completed transferring %s bytes...", transfer.getProgress().getBytesTransferred()));
        } catch (InterruptedException e) {
            return false;
        } catch (AmazonS3Exception as3e) {
            throw new MojoFailureException("Error uploading to S3", as3e);
        }

        return true;
    }

    @Override
    public void provideObjectMetadata(File file, ObjectMetadata objectMetadata) {
        getLog().debug(String.format("Creating metadata for %s (size=%s)", file.getName(), file.length()));

        if (drigoResults == null) {
            throw new RuntimeException("Cant happen - no drigo results found");
        }

        Map<String, String> meta = drigoResults.getMetadata().get(file);
        getLog().debug("For file " + file + " got " + meta);
        if (meta != null) {
            ObjectMetadataSettingParam.populateObjectMetaDataFromDrigoMeta(objectMetadata, meta);
        }
        String uploadTimestamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss Z").format(new Date());
        objectMetadata.addUserMetadata("seedy-upload-time", uploadTimestamp);
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
