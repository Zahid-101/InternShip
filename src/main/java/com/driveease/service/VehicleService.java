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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    private final FileValidatorService fileValidatorService;
    private final OcrService ocrService;

    public VehicleService(VehicleRepository vehicleRepository,
                          DocumentRepository documentRepository,
                          S3Service s3Service,
                          FileValidatorService fileValidatorService,
                          OcrService ocrService) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
        this.fileValidatorService = fileValidatorService;
        this.ocrService = ocrService;
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
                .contractExpiryDate(request.getContractExpiryDate())
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

        // Step 1: Validate file (extension, magic bytes, extension-content match)
        fileValidatorService.validate(file);

        // Step 2: Upload to S3 and get public URL
        String s3Url = s3Service.uploadFile(file);

        // Step 3: Perform OCR (gracefully returns empty string on failure)
        String ocrText = ocrService.extractText(file);

        // Step 4: Save metadata to DB
        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .s3Url(s3Url)
                .rawOcrText(ocrText)
                .uploadDate(LocalDateTime.now())
                .vehicle(vehicle)
                .build();

        Document saved = documentRepository.save(document);
        return toDocumentResponse(saved);
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
                .contractExpiryDate(vehicle.getContractExpiryDate())
                .documents(docs)
                .build();
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .s3Url(document.getS3Url())
                .rawOcrText(document.getRawOcrText())
                .uploadDate(document.getUploadDate())
                .build();
    }
}
