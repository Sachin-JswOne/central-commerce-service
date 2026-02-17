package com.jswone.commerce.core.service.impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.jswone.commerce.core.service.GcsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGcsService implements GcsService {

    private final Storage storage;

    @Override
    public void uploadFile(String bucketName, String objectName, byte[] content, String contentType) {
        uploadFile(bucketName, objectName, new ByteArrayInputStream(content), contentType);
    }

    @Override
    public void uploadFile(String bucketName, String objectName, byte[] content, String contentType,
            String contentEncoding) {
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .setContentEncoding(contentEncoding)
                    .build();
            storage.createFrom(blobInfo, new ByteArrayInputStream(content));
            log.info("File uploaded to bucket {} as {} with encoding {}", bucketName, objectName, contentEncoding);
        } catch (Exception e) {
            log.error("Error uploading file to GCS bucket {}: {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to GCS", e);
        }
    }

    @Override
    public void uploadFile(String bucketName, String objectName, InputStream content, String contentType) {
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
            storage.createFrom(blobInfo, content);
            log.info("File uploaded to bucket {} as {}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Error uploading file to GCS bucket {}: {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to GCS", e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            boolean deleted = storage.delete(blobId);
            if (deleted) {
                log.info("File {} deleted from bucket {}", objectName, bucketName);
            } else {
                log.warn("File {} not found in bucket {}", objectName, bucketName);
            }
        } catch (Exception e) {
            log.error("Error deleting file from GCS bucket {}: {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from GCS", e);
        }
    }
}
