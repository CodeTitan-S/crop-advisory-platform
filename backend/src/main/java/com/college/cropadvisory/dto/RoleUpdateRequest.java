package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    /** {@code FARMER}, {@code OFFICER} or {@code ADMIN}. Kept as a String so an unknown value
     * yields a clean 400 from the service rather than a Jackson deserialization failure. */
    @NotBlank
    private String role;
}
