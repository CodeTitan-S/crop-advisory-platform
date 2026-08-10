package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiseaseReportRequest {
    @NotNull
    private Long farmId;
    @NotBlank
    private String description;
    private String imageUrl; // optional
}