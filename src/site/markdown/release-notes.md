## Release Notes

### Version 0.8.x
This is a backwards compatible preview release
* Switching to semantic versioning
* Switching to CircleCI for continuous integration
* Adding preview version of Lambda API deployment
* Refactoring of documentation into Maven site-worthy setup

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

### Pre-0.6
Didn't keep track of release notes before 0.6
