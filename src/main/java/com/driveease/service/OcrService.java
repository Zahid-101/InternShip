package com.driveease.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Performs OCR (TEXT_DETECTION) on uploaded documents using Google Cloud Vision API.
 *
 * <p>Explicitly loads credentials from the path specified in
 * {@code GOOGLE_APPLICATION_CREDENTIALS} (read via spring-dotenv),
 * since the .env file is not visible to Google's default credential chain.</p>
 *
 * <p>If credentials are not configured or the API call fails, this service
 * gracefully returns an empty string instead of blocking the upload.</p>
 */
@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String credentialsPath;

    /**
     * Extracts text from the given file using Google Cloud Vision TEXT_DETECTION.
     *
     * @param file the uploaded multipart file (PDF, JPG, or PNG)
     * @return the extracted text, or empty string if OCR fails or is unavailable
     */
    public String extractText(MultipartFile file) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            logger.warn("GOOGLE_APPLICATION_CREDENTIALS not set. Skipping OCR for '{}'.",
                    file.getOriginalFilename());
            return "";
        }

        try {
            // Explicitly load credentials from the file path in .env and set the required OAuth scope
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath))
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();

            try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {

                ByteString imgBytes = ByteString.readFrom(file.getInputStream());

                Image image = Image.newBuilder()
                        .setContent(imgBytes)
                        .build();

                Feature feature = Feature.newBuilder()
                        .setType(Feature.Type.TEXT_DETECTION)
                        .build();

                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feature)
                        .setImage(image)
                        .build();

                BatchAnnotateImagesResponse response = vision.batchAnnotateImages(
                        List.of(request));

                AnnotateImageResponse imageResponse = response.getResponses(0);

                if (imageResponse.hasError()) {
                    logger.warn("Vision API error for '{}': {}",
                            file.getOriginalFilename(),
                            imageResponse.getError().getMessage());
                    return "";
                }

                String fullText = imageResponse.getFullTextAnnotation().getText();
                logger.info("OCR extracted {} characters from '{}'",
                        fullText.length(), file.getOriginalFilename());
                return fullText;
            }

        } catch (Exception e) {
            logger.warn("OCR unavailable for '{}': {}. Upload will proceed without OCR.",
                    file.getOriginalFilename(), e.getMessage());
            return "";
        }
    }
}
