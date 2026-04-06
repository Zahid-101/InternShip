package com.driveease.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class BookingRequestDTO {

    @NotNull(message = "Pickup date is required")
    private LocalDate pickupDate;

    @NotNull(message = "Return date is required")
    private LocalDate returnDate;

    private String comments;

    /** Optional override — if provided, used as totalPrice instead of calculated sum */
    private BigDecimal manualPrice;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {

        @NotNull(message = "Vehicle ID is required")
        private Long vehicleId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
