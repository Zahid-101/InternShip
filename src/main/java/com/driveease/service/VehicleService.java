package com.driveease.service;

import com.driveease.dto.DocumentResponse;
import com.driveease.dto.VehicleRequest;
import com.driveease.dto.VehicleResponse;
import com.driveease.model.Document;
import com.driveease.model.Vehicle;
import com.driveease.model.VehicleType;
import com.driveease.repository.DocumentRepository;
import com.driveease.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "png");
    private static final String UPLOAD_DIR = "uploads";

    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          DocumentRepository documentRepository) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
    }

    public VehicleResponse addVehicle(VehicleRequest request) {
        VehicleType type;
        try {
            type = VehicleType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid vehicle type. Must be one of: SUV, SEDAN, HATCHBACK, OTHER");
        }

        Vehicle vehicle = Vehicle.builder()
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .type(type)
                .baseDailyRate(request.getBaseDailyRate())
                .quantityAvailable(request.getQuantityAvailable())
                .imageUrl(request.getImageUrl())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        return toResponse(vehicle);
    }

    public DocumentResponse uploadDocument(Long vehicleId, MultipartFile file) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Only .pdf, .jpg, and .png are allowed");
        }

        // Save file to uploads/ directory
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename to avoid collisions
            String storedFileName = UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Save metadata to DB
            Document document = Document.builder()
                    .fileName(originalFilename)
                    .filePath(filePath.toString())
                    .uploadDate(LocalDateTime.now())
                    .vehicle(vehicle)
                    .build();

            Document saved = documentRepository.save(document);
            return toDocumentResponse(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public List<DocumentResponse> getDocumentsByVehicleId(Long vehicleId) {
        // Verify vehicle exists
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        return documentRepository.findByVehicleId(vehicleId).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    // ---- Mapping helpers ----

    private VehicleResponse toResponse(Vehicle vehicle) {
        List<DocumentResponse> docs = vehicle.getDocuments() != null
                ? vehicle.getDocuments().stream()
                    .map(this::toDocumentResponse)
                    .collect(Collectors.toList())
                : List.of();

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .type(vehicle.getType().name())
                .baseDailyRate(vehicle.getBaseDailyRate())
                .quantityAvailable(vehicle.getQuantityAvailable())
                .imageUrl(vehicle.getImageUrl())
                .documents(docs)
                .build();
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .uploadDate(document.getUploadDate())
                .build();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
