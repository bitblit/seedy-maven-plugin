package com.erigir.maven.plugin;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.apache.maven.plugin.MojoExecutionException;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractSeedyMojo extends org.apache.maven.plugin.AbstractMojo {
    private AWSCredentials credentials;

    protected synchronized final AWSCredentials credentials()
            throws MojoExecutionException {
        if (credentials == null) {
            getLog().info("Fetching AWS Credentials");
            credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
            if (credentials == null) {
                throw new MojoExecutionException("Couldn't fetch credentials - either set aws.accessKeyId and aws.secretKey or (better) grant this machine an IAM role");
            }

            getLog().info("Using credentials : " + obscure(credentials.getAWSAccessKeyId(), 2) + " / " + obscure(credentials.getAWSSecretKey(), 2));
        }
        return credentials;
    }

    protected AmazonS3 s3()
            throws MojoExecutionException {
        return new AmazonS3Client(credentials());
    }

    protected final int buildNumber(Integer def)
            throws MojoExecutionException {
        Integer rval = null;
        String env = System.getProperty("BUILD_NUMBER");
        if (env == null) {
            if (def == null) {
                throw new MojoExecutionException("No environment variable 'BUILD_NUMBER' found and no default set");
            } else {
                getLog().info("No environment variable 'BUILD_NUMBER' found, using default (Jenkins would set this)");
                rval = def;
            }
        } else {
            rval = new Integer(env);
        }
        getLog().info("Using build number : "+rval);
        return rval;
    }

    protected final String buildId() {
        String rval = System.getProperty("BUILD_ID");
        if (rval == null) {
            getLog().info("No environment variable 'BUILD_ID' found, defaulting to timestamp (Jenkins would set this)");
            rval = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
        }
        getLog().info("Using build id : "+rval);
        return rval;
    }

    protected final void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            getLog().info("Sleep interrupted");
        }
    }

    protected final String obscure(String input, int save) {
        String rval = input;
        if (input != null && input.length() > save * 2) {
            StringBuilder b = new StringBuilder();
            b.append(input.substring(0, save));
            while (b.length() < input.length() - save) {
                b.append("*");
            }
            b.append(input.substring(input.length() - save));
            rval = b.toString();
        }
        return rval;
    }

}

