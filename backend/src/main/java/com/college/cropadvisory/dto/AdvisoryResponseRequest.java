package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body of {@code PUT /api/advisory-requests/{id}/respond}.
 *
 * <p>A DTO rather than a {@code Map}: the field is declared once and validated by {@code @Valid},
 * so a blank response is rejected before it reaches the service.
 */
@Data
public class AdvisoryResponseRequest {

    @NotBlank
    private String responseText;
}
