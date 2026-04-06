package com.driveease.service;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Validates uploaded files by checking both the file extension and the actual
 * file content (magic bytes) using Apache Tika.
 *
 * <p>Only {@code application/pdf}, {@code image/jpeg}, and {@code image/png}
 * are permitted. A {@link SecurityException} is thrown if the file is invalid
 * or if the extension does not match the detected content type.</p>
 */
@Service
public class FileValidatorService {

    private static final Logger logger = LoggerFactory.getLogger(FileValidatorService.class);
    private static final Tika tika = new Tika();

    /** Allowed MIME types that may be uploaded. */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    /**
     * Maps each allowed file extension to the MIME type it MUST match.
     * This prevents attacks like renaming a .exe to .pdf.
     */
    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
            "pdf",  "application/pdf",
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png"
    );

    /**
     * Validates the given file. Throws {@link SecurityException} if:
     * <ul>
     *   <li>The filename is missing or blank</li>
     *   <li>The extension is not in the allowed set</li>
     *   <li>The actual content (magic bytes) is not an allowed MIME type</li>
     *   <li>The extension does not match the detected content type</li>
     * </ul>
     *
     * @param file the uploaded multipart file
     * @throws SecurityException if file validation fails
     */
    public void validate(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        // 1. Filename must be present
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new SecurityException("File name is missing");
        }

        // 2. Extension must be in the allowed set
        String extension = getFileExtension(originalFilename).toLowerCase();
        String expectedMime = EXTENSION_TO_MIME.get(extension);

        if (expectedMime == null) {
            throw new SecurityException(
                    "Invalid file extension '." + extension + "'. "
                    + "Only .pdf, .jpg, .jpeg, and .png are allowed");
        }

        // 3. Detect actual content type via Tika magic bytes
        String detectedMime;
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            detectedMime = tika.detect(is);
        } catch (IOException e) {
            throw new SecurityException(
                    "Failed to read file for validation: " + e.getMessage());
        }

        // 4. Detected MIME must be in the allowed set
        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            logger.warn("Rejected file '{}': detected MIME '{}' is not allowed",
                    originalFilename, detectedMime);
            throw new SecurityException(
                    "File content is not an allowed type. Detected: " + detectedMime);
        }

        // 5. Extension must match the actual content
        if (!expectedMime.equals(detectedMime)) {
            logger.warn("Rejected file '{}': extension implies '{}' but content is '{}'",
                    originalFilename, expectedMime, detectedMime);
            throw new SecurityException(
                    "File extension '." + extension + "' does not match the actual content type '"
                    + detectedMime + "'. Possible file spoofing detected");
        }

        logger.debug("File '{}' passed validation ({})", originalFilename, detectedMime);
    }

    /**
     * Validates that the file is specifically an image (JPEG or PNG only).
     * PDFs are not allowed for image uploads.
     */
    public void validateImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new SecurityException("File name is missing");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        String expectedMime = EXTENSION_TO_MIME.get(extension);

        if (expectedMime == null || !expectedMime.startsWith("image/")) {
            throw new SecurityException(
                    "Invalid file extension '." + extension + "'. "
                    + "Only .jpg, .jpeg, and .png are allowed for images");
        }

        String detectedMime;
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            detectedMime = tika.detect(is);
        } catch (IOException e) {
            throw new SecurityException(
                    "Failed to read file for validation: " + e.getMessage());
        }

        if (!detectedMime.startsWith("image/") || !ALLOWED_MIME_TYPES.contains(detectedMime)) {
            logger.warn("Rejected image '{}': detected MIME '{}' is not an allowed image type",
                    originalFilename, detectedMime);
            throw new SecurityException(
                    "File is not a valid image. Detected: " + detectedMime);
        }

        if (!expectedMime.equals(detectedMime)) {
            logger.warn("Rejected image '{}': extension implies '{}' but content is '{}'",
                    originalFilename, expectedMime, detectedMime);
            throw new SecurityException(
                    "File extension '." + extension + "' does not match the actual content type '"
                    + detectedMime + "'");
        }

        logger.debug("Image '{}' passed validation ({})", originalFilename, detectedMime);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }
}
