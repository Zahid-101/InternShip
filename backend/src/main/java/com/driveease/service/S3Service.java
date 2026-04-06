package com.driveease.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;

    @Value("${driveease.s3.bucket-name}")
    private String bucketName;

    @Value("${driveease.s3.region}")
    private String region;

    @Value("${driveease.s3.endpoint:#{null}}")
    private String endpoint;

    @Value("${driveease.s3.path-style-access}")
    private boolean pathStyleAccess;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a document file to S3 under the "documents/" prefix.
     */
    public String uploadFile(MultipartFile file) {
        return upload(file, "documents");
    }

    /**
     * Uploads a vehicle profile image to S3 under the "images/" prefix.
     */
    public String uploadImage(MultipartFile file) {
        return upload(file, "images");
    }

    private String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String key = folder + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = buildPublicUrl(key);
            logger.info("File uploaded to S3: {}", url);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the public URL for the uploaded object.
     * Dev (LocalStack): http://localhost:4566/bucket/key
     * Prod (AWS):       https://bucket.s3.region.amazonaws.com/key
     */
    private String buildPublicUrl(String key) {
        if (endpoint != null && !endpoint.isBlank()) {
            // LocalStack / path-style
            return endpoint + "/" + bucketName + "/" + key;
        }
        // Standard AWS virtual-hosted style
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }
}
