package com.blackduck.integration.sca.upload.file;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.body.StringBodyContent;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadStartRequestData;
import com.blackduck.integration.sca.upload.file.model.MultipartUrlData;
import org.apache.http.entity.ContentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class XmlApiFileUploader extends AbstractFileUploader {

    public XmlApiFileUploader(IntHttpClient httpClient, int multipartUploadPartRetryAttempts, long multipartUploadPartRetryInitialInterval, int multipartUploadTimeoutInMinutes, ExecutorService executorService) {
        super(httpClient, multipartUploadPartRetryAttempts, multipartUploadPartRetryInitialInterval, multipartUploadTimeoutInMinutes, executorService);
    }

    @Override
    protected HttpUrl getUploadUrl() throws IntegrationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Request getMultipartUploadStartRequest(MultipartUploadStartRequestData uploadStartRequestData) throws IntegrationException {
        Map<String, String> startHeaders = uploadStartRequestData.getHeaders();
        HttpUrl requestUrl = new HttpUrl(uploadStartRequestData.getBaseUrl());

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(startHeaders)
                .method(HttpMethod.POST);

        return builder.build();
    }

    @Override
    protected Request.Builder getMultipartUploadPartRequestBuilder(MultipartUploadFileMetadata fileMetaData, String uploadUrl, MultipartUploadFilePart part) throws IntegrationException {
        Map<String, String> allHeaders = part.getPartUploadData().getHeaders();
        HttpUrl partUrl = new HttpUrl(part.getPartUploadData().getUrl());

        return new Request.Builder()
                .url(partUrl)
                .method(HttpMethod.PUT)
                .headers(allHeaders);
    }

    @Override
    protected Request getMultipartUploadFinishRequest(String uploadUrl, Map<Integer, String> tagOrderMap, MultipartUrlData completeUploadUrl) throws IntegrationException {
        Map<String, String> finishHeaders = completeUploadUrl.getHeaders();
        HttpUrl requestUrl = new HttpUrl(completeUploadUrl.getUrl());

        try {
            StringWriter writer = new StringWriter();
            XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);

            xmlWriter.writeStartElement("CompleteMultipartUpload");

            for (Map.Entry<Integer, String> entry : tagOrderMap.entrySet()) {
                xmlWriter.writeStartElement("Part");
                xmlWriter.writeStartElement("PartNumber");
                xmlWriter.writeCharacters(entry.getKey().toString());
                xmlWriter.writeEndElement();
                xmlWriter.writeStartElement("ETag");
                xmlWriter.writeCharacters(entry.getValue());
                xmlWriter.writeEndElement();
                xmlWriter.writeEndElement();
            }

            xmlWriter.writeEndElement();
            xmlWriter.writeEndDocument();
            xmlWriter.close();

            String xmlBody = writer.toString();

            StringBodyContent bodyContent = new StringBodyContent(xmlBody, ContentType.APPLICATION_XML);

            Request.Builder builder = new Request.Builder()
                    .url(requestUrl)
                    .headers(finishHeaders)
                    .bodyContent(bodyContent)
                    .method(HttpMethod.POST);

            return builder.build();
        } catch (Exception e) {
            throw new IntegrationException("Error creating multipart upload URL", e);
        }
    }
}
