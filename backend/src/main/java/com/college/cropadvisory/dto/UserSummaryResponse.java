package com.college.cropadvisory.dto;

import com.college.cropadvisory.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Admin-facing view of a user. Deliberately never carries {@code passwordHash}, so an admin
 * listing can never leak credential material.
 *
 * <p>The farm count is passed in rather than read from the entity: with
 * {@code spring.jpa.open-in-view=false} (the {@code prod} profile) touching a lazy collection
 * outside a transaction would throw.
 */
@Data
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private long farmCount;

    public static UserSummaryResponse of(User user, long farmCount) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                farmCount);
    }
}
