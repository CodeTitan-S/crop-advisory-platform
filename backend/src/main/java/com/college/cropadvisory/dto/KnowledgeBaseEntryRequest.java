package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseEntryRequest {

    @NotBlank
    @Size(max = 150)
    private String cropOrDiseaseName;

    @Size(max = 2000)
    private String description;

    @Size(max = 2000)
    private String remedyOrAdvice;

    @Size(max = 300)
    private String source;
}
