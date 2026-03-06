package com.driveease.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {

    @NotBlank(message = "Make is required")
    private String make;

    @NotBlank(message = "Model is required")
    private String model;

    @Min(value = 1900, message = "Year must be 1900 or later")
    private int year;

    @NotBlank(message = "Type is required")
    private String type;

    @NotNull(message = "Base daily rate is required")
    @DecimalMin(value = "0.01", message = "Base daily rate must be greater than 0")
    private BigDecimal baseDailyRate;

    @Min(value = 0, message = "Quantity must be 0 or more")
    private int quantityAvailable;

    private String imageUrl;
}
