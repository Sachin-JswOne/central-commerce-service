package com.jswone.commerce.core.service;

import java.io.InputStream;

public interface GcsService {


    void uploadFile(String bucketName, String objectName, byte[] content, String contentType, String contentEncoding);

    void uploadFile(String bucketName, String objectName, InputStream content, String contentType);

    void deleteFile(String bucketName, String objectName);
}
