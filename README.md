# Overview  
  
The blackduck-upload-common library provides convenient tools to facilitate the usage of the upload and multipart upload Black Duck REST APIs endpoints. 

Based on the file size of the target upload file, the library will either perform a standard upload or a multipart upload. In the case of a multipart upload, this library splits files into chunks and are uploaded separately. Once all chunks are finished uploading a final request is sent to reassemble the file in Black Duck to be validated and scanned. 

# Supported Upload Types:

- Binary
  
# Getting Started:  
Clients of this library will create an uploader which manages standard and multipart uploads. The supported Uploaders are listed above. 

To begin, a client of this library should determine which uploader to instantiate using the available providers in `UploaderFactory.java`. 

To set up this factory, clients must create an instance of the `UploaderConfig.java` object containing information required for authentication with Black Duck, as well as additional fields for upload settings.

A full list of supported properties is available in `EnvironmentProperties.java`.