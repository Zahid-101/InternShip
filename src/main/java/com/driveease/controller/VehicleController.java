package com.driveease.controller;

import com.driveease.dto.DocumentResponse;
import com.driveease.dto.VehicleRequest;
import com.driveease.dto.VehicleResponse;
import com.driveease.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getDocumentsByVehicleId(id));
    }
}
