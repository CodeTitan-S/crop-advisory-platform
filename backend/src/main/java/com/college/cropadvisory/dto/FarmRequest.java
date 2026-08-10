package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FarmRequest {
    @NotBlank
    private String location;
    @NotNull
    private Double size;
    @NotBlank
    private String soilType;
}