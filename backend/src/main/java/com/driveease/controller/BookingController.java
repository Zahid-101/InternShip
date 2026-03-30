package com.driveease.controller;

import com.driveease.dto.BookingRequestDTO;
import com.driveease.dto.BookingResponseDTO;
import com.driveease.exception.InsufficientStockException;
import com.driveease.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/fleet")
    public ResponseEntity<?> createFleetBooking(@Valid @RequestBody BookingRequestDTO request,
                                                 Authentication authentication) {
        try {
            String agentUsername = authentication.getName();
            BookingResponseDTO response = bookingService.createFleetBooking(request, agentUsername);
            return ResponseEntity.ok(response);
        } catch (InsufficientStockException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
