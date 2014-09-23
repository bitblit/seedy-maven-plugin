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
import java.util.regex.Pattern;

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
     * List of object metadata settings
     */
    @Parameter(property = "s3-upload.objectMetadataSettings")
    List<ObjectMetadataSetting> objectMetadataSettings;

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

    public void applyProcessorToFile(File src, FileProcessor processor, Pattern matching)
            throws MojoExecutionException
    {
        assert(src!=null && processor!=null);
        if (src.isFile())
        {
            if (matching==null || matching.matcher(src.getAbsolutePath()).matches())
            {
                getLog().info("Applying "+processor.getClass().getName()+" to "+src);
                processor.process(getLog(),src);
            }
        }
        else
        {
            for (String s:src.list())
            {
                applyProcessorToFile(new File(src,s),processor,matching);
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (objectMetadataSettings==null)
        {
            getLog().info("No upload configs specified, using default");
            objectMetadataSettings = new LinkedList<>();
        }

        if (fileCompression!=null && fileCompression.getIncludeRegex()!=null)
        {
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
        FileProcessorUtils.copyFolder(sourceFile,myTemp);

        // Now, apply Css compression if applicable
        getLog().info("Checking CSS compression");
        if (cssCompilation!=null && cssCompilation.getIncludeRegex()!=null)
        {
            if (cssCompilation.isCombine())
            {
                getLog().info("CSS Combination not yet implemented");
            }
            YUICompileContentModelProcessor proc = new YUICompileContentModelProcessor();
            applyProcessorToFile(myTemp, proc, Pattern.compile(cssCompilation.getIncludeRegex()));
        }

        getLog().info("Checking JS compression");
        if (javascriptCompilation!=null && javascriptCompilation.getIncludeRegex()!=null)
        {
            if (javascriptCompilation.isCombine())
            {
                getLog().info("Javascript combination not yet implemented");
            }
            InProcessClosureCompiler.disableSystemExit(getLog());
            JavascriptCompilerFileProcessor ipcc = new JavascriptCompilerFileProcessor();
            try {
                applyProcessorToFile(myTemp, ipcc, Pattern.compile(javascriptCompilation.getIncludeRegex()));
            }
            catch (Throwable t)
            {
                getLog().error("Caught "+t);
                throw t;
            }
            InProcessClosureCompiler.enableSystemExit(getLog());
        }

        getLog().info("Checking GZIP compression");
        if (fileCompression!=null && fileCompression.getIncludeRegex()!=null)
        {
            GZipFileProcessor gzfp = new GZipFileProcessor();
            applyProcessorToFile(myTemp, gzfp, Pattern.compile(fileCompression.getIncludeRegex()));
            getLog().info("GZIP compression saved "+GZipFileProcessor.totalSaved+" bytes in total");
        }

        if (doNotUpload) {
            getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run)",
                    myTemp, s3Bucket, s3Prefix));

            return;
        }

        getLog().info("About to being upload of files");
        boolean success = upload(s3, myTemp);
        if (!success) {
            throw new MojoExecutionException("Unable to upload file to S3.");
        }

        getLog().info(String.format("File %s uploaded to s3://%s/%s",
                sourceFile, s3Bucket, s3Prefix));
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

}
