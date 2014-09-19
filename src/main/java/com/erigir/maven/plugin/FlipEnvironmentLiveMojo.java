package com.erigir.maven.plugin;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "flip-environment", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class FlipEnvironmentLiveMojo extends AbstractSeedyMojo {
    /**
     * Application Name in elastic beanstalk
     */
    @Parameter(property = "flip-environment.applicationName", required = true)
    private String applicationName;

    /**
     * Machine type
     */
    @Parameter(property = "flip-environment.liveServerDomainName", required = true)
    private String liveServerDomainName;

    /**
     * Max Seconds to wait before giving up
     */
    @Parameter(property = "flip-environment.preFlipLiveWaitSeconds", defaultValue = "15")
    private int preFlipLiveWaitSeconds;

    /**
     * Machine type
     */
    @Parameter(property = "flip-environment.terminateOldEnviroment", defaultValue = "false")
    private boolean terminateOldEnviroment;

    @Override
    public void execute() throws MojoExecutionException {
        int buildNumber = buildNumber(1);
        String environmentName = applicationName + "-" + buildNumber;

        getLog().info("Finding current live url");
        AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient(credentials());
        EnvironmentDescription liveEnvironment = findEnvironmentByCNAME(eb, liveServerDomainName);

        getLog().info("Swapping new version live (old live is " + liveEnvironment + ")");
        SwapEnvironmentCNAMEsRequest swap = new SwapEnvironmentCNAMEsRequest().withSourceEnvironmentName(liveEnvironment.getEnvironmentName()).withDestinationEnvironmentName(environmentName);
        eb.swapEnvironmentCNAMEs(swap);

        if (terminateOldEnviroment) {
            getLog().info("Waiting " + preFlipLiveWaitSeconds + " seconds post live before terminating old environment");
            safeSleep(preFlipLiveWaitSeconds * 1000);

            getLog().info("Terminating old environment");
            TerminateEnvironmentRequest ter = new TerminateEnvironmentRequest().withEnvironmentName(liveEnvironment.getEnvironmentName());
            eb.terminateEnvironment(ter);
        }

        getLog().info("Flip live is now complete");
    }

    private EnvironmentDescription findEnvironmentByCNAME(AWSElasticBeanstalkClient eb, String domainName) {
        DescribeEnvironmentsResult res = eb.describeEnvironments();

        EnvironmentDescription rval = null;
        for (EnvironmentDescription ed : res.getEnvironments()) {
            if (ed.getCNAME().equals(domainName)) {
                rval = ed;
            }
        }
        return rval;
    }

}

