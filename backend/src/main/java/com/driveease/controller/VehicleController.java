package com.driveease.controller;

import com.driveease.dto.DocumentResponse;
import com.driveease.dto.PriceRequest;
import com.driveease.dto.PriceResponse;
import com.driveease.dto.VehicleRequest;
import com.driveease.dto.VehicleResponse;
import com.driveease.exception.InsufficientStockException;
import com.driveease.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> addVehicle(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.addVehicle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get all vehicles",
            description = "Retrieves a list of all vehicles available in the system."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved list of vehicles")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @PostMapping("/calculate-price")
    public ResponseEntity<?> calculatePrice(@Valid @RequestBody PriceRequest request) {
        try {
            PriceResponse response = vehicleService.calculatePrice(request);
            return ResponseEntity.ok(response);
        } catch (InsufficientStockException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    public ResponseEntity<Page<VehicleResponse>> searchVehicles(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
            @RequestParam(required = false) Integer rentalDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleResponse> results = vehicleService.searchVehicles(
                name, type, quantity, pickupDate, rentalDays, PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file) {
        try {
            DocumentResponse response = vehicleService.uploadDocument(id, file);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVehicleImage(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file) {
        try {
            VehicleResponse response = vehicleService.uploadVehicleImage(id, file);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Image upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getDocumentsByVehicleId(id));
    }
}
