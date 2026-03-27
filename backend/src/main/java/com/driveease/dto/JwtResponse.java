package com.driveease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private String username;

    private String role;
}
