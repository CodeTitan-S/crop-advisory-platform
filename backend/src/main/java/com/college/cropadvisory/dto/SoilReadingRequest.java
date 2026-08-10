package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SoilReadingRequest {
    @NotNull
    private Double nitrogen;
    @NotNull
    private Double phosphorus;
    @NotNull
    private Double potassium;
    @NotNull
    private Double ph;
    @NotNull
    private Double rainfall;
    @NotNull
    private Double temperature;
}