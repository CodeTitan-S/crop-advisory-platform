package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body of {@code PUT /api/disease-reports/{id}/resolve}.
 *
 * <p>`resolutionNotes` was previously optional at the controller and only stored as-is; the notes
 * are what the farmer reads, so a blank value is now a 400 rather than an empty resolution.
 */
@Data
public class DiseaseResolutionRequest {

    @NotBlank
    private String resolutionNotes;
}
