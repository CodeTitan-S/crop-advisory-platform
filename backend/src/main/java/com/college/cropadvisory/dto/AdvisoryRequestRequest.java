package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdvisoryRequestRequest {
    @NotNull
    private Long farmId;
    @NotBlank
    private String questionText;
}