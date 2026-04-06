package com.driveease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;
    private String bookingRef;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private BigDecimal totalPrice;
    private String status;
    private String agentUsername;
    private String agentComments;
    private List<BookingItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingItemResponse {
        private Long vehicleId;
        private String vehicleName;
        private Integer quantity;
        private BigDecimal priceAtBooking;
    }
}
