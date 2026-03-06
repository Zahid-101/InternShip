package com.driveease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private Long id;
    private String make;
    private String model;
    private int year;
    private String type;
    private BigDecimal baseDailyRate;
    private int quantityAvailable;
    private String imageUrl;
    private List<DocumentResponse> documents;
}
