package com.driveease.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceRequest {

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Rental days is required")
    @Min(value = 1, message = "Rental days must be at least 1")
    private Integer rentalDays;

    @NotNull(message = "Quantity requested is required")
    @Min(value = 1, message = "Quantity requested must be at least 1")
    private Integer quantityRequested;
}
