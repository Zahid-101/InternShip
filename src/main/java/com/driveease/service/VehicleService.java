package com.driveease.service;

import com.driveease.dto.DocumentResponse;
import com.driveease.dto.PriceRequest;
import com.driveease.dto.PriceResponse;
import com.driveease.dto.VehicleRequest;
import com.driveease.dto.VehicleResponse;
import com.driveease.exception.InsufficientStockException;
import com.driveease.model.Document;
import com.driveease.model.Vehicle;
import com.driveease.model.VehicleType;
import com.driveease.repository.DocumentRepository;
import com.driveease.repository.VehicleRepository;
import com.driveease.specification.VehicleSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    /** 10% markup on the base daily rate */
    private static final BigDecimal RENTAL_MARKUP_RATE = new BigDecimal("1.10");

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

    /**
     * Calculates the rental price for a vehicle.
     * Formula: Total = (baseDailyRate × 1.10) × rentalDays × quantityRequested
     */
    public PriceResponse calculatePrice(PriceRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException(
                        "Vehicle not found with id: " + request.getVehicleId()));

        // Stock check
        if (request.getQuantityRequested() > vehicle.getQuantityAvailable()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Requested: " + request.getQuantityRequested()
                    + ", Available: " + vehicle.getQuantityAvailable());
        }

        BigDecimal baseDailyRate = vehicle.getBaseDailyRate();
        BigDecimal rentalDays = BigDecimal.valueOf(request.getRentalDays());
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantityRequested());

        // markupAmount = baseDailyRate × 0.10
        BigDecimal markupAmount = baseDailyRate
                .multiply(RENTAL_MARKUP_RATE.subtract(BigDecimal.ONE))
                .setScale(2, RoundingMode.HALF_UP);

        // subtotal = baseDailyRate × rentalDays × quantity (before markup)
        BigDecimal subtotal = baseDailyRate
                .multiply(rentalDays)
                .multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);

        // grandTotal = (baseDailyRate × MARKUP) × rentalDays × quantity
        BigDecimal grandTotal = baseDailyRate
                .multiply(RENTAL_MARKUP_RATE)
                .multiply(rentalDays)
                .multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);

        return PriceResponse.builder()
                .baseDailyRate(baseDailyRate)
                .markupAmount(markupAmount)
                .subtotal(subtotal)
                .grandTotal(grandTotal)
                .build();
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

    public Page<VehicleResponse> searchVehicles(String name, String type, Integer quantity,
                                                 LocalDate pickupDate, Integer rentalDays,
                                                 Pageable pageable) {
        Specification<Vehicle> spec = VehicleSpecification.buildSearch(
                name, type, quantity, pickupDate, rentalDays);
        return vehicleRepository.findAll(spec, pageable).map(this::toResponse);
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
