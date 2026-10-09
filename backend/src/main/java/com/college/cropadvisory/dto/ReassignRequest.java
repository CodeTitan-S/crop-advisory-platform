package com.college.cropadvisory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Body for the admin reassignment endpoint: the officer to hand the work item to. */
@Data
public class ReassignRequest {
    @NotNull
    private Long officerId;
}
