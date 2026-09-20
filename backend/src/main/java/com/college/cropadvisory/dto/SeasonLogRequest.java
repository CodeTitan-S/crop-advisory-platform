package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SeasonLogRequest {

    @NotBlank
    @Size(max = 100)
    private String cropPlanted;

    /** A season log records what was already sown, so the date cannot be in the future. */
    @NotNull
    @PastOrPresent
    private LocalDate sowingDate;

    @Size(max = 2000)
    private String outcomeNotes;
}
