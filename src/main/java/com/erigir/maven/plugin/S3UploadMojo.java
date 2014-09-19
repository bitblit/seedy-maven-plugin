package com.erigir.maven.plugin;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@Mojo(name = "s3-upload")
public class S3UploadMojo extends AbstractSeedyMojo implements ObjectMetadataProvider {
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
     * List of upload configuration objects
     */
    @Parameter(property = "s3-upload.uploaduploadConfigs")
    List<UploadConfig> uploadConfigs;

    @Override
    public void execute() throws MojoExecutionException {
        if (uploadConfigs == null || uploadConfigs.isEmpty()) {
            getLog().info("No uploadConfigs specified, using default");
            uploadConfigs = Arrays.asList(new UploadConfig());
        }


        File sourceFile = new File(source);
        if (!sourceFile.exists()) {
            throw new MojoExecutionException("File/folder doesn't exist: " + source);
        }

        AmazonS3 s3 = s3();
        if (endpoint != null) {
            s3.setEndpoint(endpoint);
        }

        if (!s3.doesBucketExist(s3Bucket)) {
            throw new MojoExecutionException("Bucket doesn't exist: " + s3Bucket);
        }

        if (doNotUpload) {
            getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run)",
                    sourceFile, s3Bucket, s3Prefix));

            return;
        }

        getLog().info("Compressing any marked files prior to upload");
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File compressed = (sourceFile.isFile()) ? new File(tempDir, sourceFile.getName()) : new File(tempDir, UUID.randomUUID().toString());
        long saved = compress(sourceFile, compressed);
        getLog().info(String.format("Saved a total of %s bytes via compression", saved));
        sourceFile = compressed;
        compressed.deleteOnExit();

        boolean success = upload(s3, sourceFile);
        if (!success) {
            throw new MojoExecutionException("Unable to upload file to S3.");
        }

        getLog().info(String.format("File %s uploaded to s3://%s/%s",
                sourceFile, s3Bucket, s3Prefix));
    }

    private long compress(File from, File to)
            throws MojoExecutionException {
        long savings = 0;
        if (from == null || to == null) {
            throw new MojoExecutionException("From and To both must be non-null (from=" + from + ", to=" + to + ")");
        }

        // We have to copy the file since we will be reading from a different directory
        if (from.isFile()) {
            try {
                byte[] buffer = new byte[1024];
                OutputStream out = null;
                if (shouldCompress(from)) {
                    out = new GZIPOutputStream(new FileOutputStream(to));
                } else {
                    out = new FileOutputStream(to);
                }

                FileInputStream in =
                        new FileInputStream(from);
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }

                in.close();

                out.close();
                savings = from.length() - to.length();
            } catch (IOException ioe) {
                throw new MojoExecutionException("Error attempting to zip file", ioe);
            }
        } else // directory
        {
            to.mkdirs();

            for (String s : from.list()) {
                savings += compress(new File(from, s), new File(to, s));
            }
            getLog().debug(String.format("Saved %s bytes on directory %s", savings, from));
            return savings;
        }
        getLog().debug(String.format("Saved %s bytes on %s", savings, from));
        return savings;
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
        getLog().debug(String.format("Creating metadata for %s (size=%s)", file, file.length()));

        // Ok, cheesy, I know
        Map<String, String> fullMetaJacket = new TreeMap<>();

        for (UploadConfig e : uploadConfigs) {
            if (e.shouldInclude(file,getLog()) && !e.shouldExclude(file,getLog())) {
                getLog().info("Applying config " + e);
                e.update(objectMetadata);
            }
        }

        if (shouldCompress(file)) {
            getLog().info("Forcing content encoding to gzip since this file was compressed");
            objectMetadata.setContentEncoding("gzip");
        }
    }

    /**
     * This returns true if there exists at least 1 config saying to compress the file
     * and no uploadConfigs saying not to
     * NOTE: Yes, I'm well aware it does a bunch of recalculation of the pattern objects
     * and linear searches multiple times (and doesn't shortcut the loop), but it is simple
     * and this runs per-build.  Not really trying to save the 14 nanoseconds involved.
     *
     * @param f
     * @return
     */
    private boolean shouldCompress(File f) {
        boolean includeAndCompressFound = false;
        boolean excludeFound = false;
        for (int i = 0; i < uploadConfigs.size() && !excludeFound; i++) {
            UploadConfig u = uploadConfigs.get(i);
            includeAndCompressFound = includeAndCompressFound | (u.isCompress() && u.shouldInclude(f,getLog()));
            excludeFound = excludeFound | u.shouldExclude(f,getLog());
        }
        return includeAndCompressFound & !excludeFound;
    }




}
