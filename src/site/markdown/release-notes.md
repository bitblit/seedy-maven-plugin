## Release Notes

### Version 0.9.1
This is a backwards compatible bug-fix release
* Adds null checks around the options on s3-upload to prevent crashing on NPE when missing fields

### Version 0.9.0
This is a backwards compatible preview release
* Switching to semantic versioning
* Switching to CircleCI for continuous integration
* Adding preview version of Lambda API deployment
* Refactoring of documentation into Maven site-worthy setup

### Version 0.8.39
This is a broken version - DON'T USE IT
* Failed in attempt to move to semantic versioning atop CircleCI

### Version 0.71
This is a backwards compatible bugfix release
* Version 0.7 didn't read all pages of an S3 bucket on delta calculation - fixed in this version
* Reduced some logging spewage as well

### Version 0.7
This is a backwards compatible feature release
* Added ability to do Delta comparisons and only upload files that are different
* Added ability to delete files on S3 that don't exist locally
* Added ability to compress HTML
* Added ability to do general search-and-replace in uploaded text
* Added ability to compute MD5 and stick into a metadata field

### Version 0.6
This is a backwards compatible feature release
* Extracted file processing from S3 Upload into its own library so it can be used standalone (Drigo, in wrench-drigo)
* Added ability to run Babel processing prior to s3 upload (mainly for JSX processing)
* Added ability to exclude files from upload
* Added dynamic include capability (parse files and replace tags with contents of other files, like server side includes)
* Finally started to fix this documentation

### Version 0.5
This is a maintenance release. (2015-03-30)
* Upgraded Closure to version v20150315, and fixed it just crashing on some input - now bad input may result in 
uncompressed JS but everything should still work.  Also pays attention to the compilation level now, and no longer has
 to disable system.exit to work.  At some point will extend this to allow custom externs, but not right now.

### Version 0.4
This is a backwards compatible feature release. (2015-03-07)
* Added the 'assumedRoleArn' and 'assumedRoleExternalId' properties to all mojos to allow Seedy to deploy to a 
different AWS account than the one on the current machine.  Typical use case is when the build server is on
 account but is deploying to another account.  See 
 http://docs.aws.amazon.com/IAM/latest/UserGuide/roles-usingrole-switchapi.html for some details.

### Version 0.3 skipped

### Version 0.2
This is a maintenance release. (2015-03-03)
* Fixed an issue where Seedy was only looking at Java system properties for BUILD_ID and BUILD_NUMBER (Jenkins uses 
environmental variables)

### Version 0.1
Initial release (2014-10-20)
* This is the initial release of the Seedy product.
