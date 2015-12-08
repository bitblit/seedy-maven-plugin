# General Usage

## General Notes
Seedy ALWAYS uses the DefaultAWSCredentialProvider, and doesn't let you set a key/secret via properties.  Why?
Because you probably check your POM file into source control, and checking your keys into source control
is a really bad idea (TM), so I'd like to save you from doing that.  Of course, you could still put them
in using some Maven tricks, but there is only so far I can go to stop you from shooting yourself in the
foot, metaphorically (or physically, for that matter).

## IAM Configuration For Seedy User/Role
See seedy-iam-permissions.json

## Common Configuration Parameters
These parameters are used by all the targets, since they define how your machine interacts with AWS
*assumedRoleArn* : ARN of an AWS role to assume for the deployment.  Typically used when the 
build server running seedy belongs to a different AWS account than the one hosting the deployment.
*assumedRoleExternalId*: External IS of an AWS role to assume for the deployment.  Typically used when the 
build server running seedy belongs to a different AWS account than the one hosting the deployment.