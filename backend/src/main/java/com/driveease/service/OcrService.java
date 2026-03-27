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
import java.util.List;

/**
 * Performs OCR on uploaded documents using Google Cloud Vision API.
 *
 * <p>Handles both <b>images</b> (JPG, PNG) via {@code batchAnnotateImages}
 * and <b>PDFs</b> via {@code batchAnnotateFiles} with explicit MIME type.
 * The Vision API rejects raw PDF bytes sent through the image endpoint
 * with "Bad image data", so PDFs must use a separate code path.</p>
 */
@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String credentialsPath;

    /**
     * Extracts text from the given file using Google Cloud Vision.
     * Automatically detects PDF vs image and uses the appropriate API.
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
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath))
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            // Force token refresh to surface clock-sync / proxy errors cleanly
            credentials.refreshIfExpired();

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();

            try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
                ByteString fileBytes = ByteString.readFrom(file.getInputStream());
                String contentType = file.getContentType();

                String fullText;
                if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                    fullText = extractFromPdf(vision, fileBytes, file.getOriginalFilename());
                } else {
                    fullText = extractFromImage(vision, fileBytes, file.getOriginalFilename());
                }

                if (fullText.isEmpty()) {
                    logger.info("No text found in '{}'.", file.getOriginalFilename());
                } else {
                    logger.info("OCR extracted {} characters from '{}'",
                            fullText.length(), file.getOriginalFilename());
                }

                return fullText;
            }

        } catch (Exception e) {
            logger.warn("OCR unavailable for '{}': {}. Upload will proceed without OCR.",
                    file.getOriginalFilename(), e.getMessage());
            return "";
        }
    }

    /**
     * Handles image files (JPG, PNG) via batchAnnotateImages.
     */
    private String extractFromImage(ImageAnnotatorClient vision, ByteString imgBytes, String filename) {
        Image image = Image.newBuilder()
                .setContent(imgBytes)
                .build();

        Feature feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();

        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
        AnnotateImageResponse imageResponse = response.getResponses(0);

        if (imageResponse.hasError()) {
            logger.warn("Vision API error for '{}': {}", filename,
                    imageResponse.getError().getMessage());
            return "";
        }

        // Check fullTextAnnotation first, then fall back to textAnnotations
        if (imageResponse.hasFullTextAnnotation()) {
            return imageResponse.getFullTextAnnotation().getText();
        } else if (!imageResponse.getTextAnnotationsList().isEmpty()) {
            return imageResponse.getTextAnnotations(0).getDescription();
        }
        return "";
    }

    /**
     * Handles PDF files via batchAnnotateFiles with explicit MIME type.
     * Supports up to 5 pages per request (Vision API limit for synchronous calls).
     */
    private String extractFromPdf(ImageAnnotatorClient vision, ByteString pdfBytes, String filename) {
        InputConfig inputConfig = InputConfig.newBuilder()
                .setMimeType("application/pdf")
                .setContent(pdfBytes)
                .build();

        Feature feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();

        AnnotateFileRequest fileRequest = AnnotateFileRequest.newBuilder()
                .setInputConfig(inputConfig)
                .addFeatures(feature)
                .build();

        BatchAnnotateFilesResponse response = vision.batchAnnotateFiles(List.of(fileRequest));
        AnnotateFileResponse fileResponse = response.getResponses(0);

        if (fileResponse.hasError()) {
            logger.warn("Vision API error for '{}': {}", filename,
                    fileResponse.getError().getMessage());
            return "";
        }

        // Concatenate text from all pages
        StringBuilder allText = new StringBuilder();
        for (AnnotateImageResponse pageResponse : fileResponse.getResponsesList()) {
            if (pageResponse.hasFullTextAnnotation()) {
                allText.append(pageResponse.getFullTextAnnotation().getText());
            } else if (!pageResponse.getTextAnnotationsList().isEmpty()) {
                allText.append(pageResponse.getTextAnnotations(0).getDescription());
            }
        }

        return allText.toString();
    }
}
