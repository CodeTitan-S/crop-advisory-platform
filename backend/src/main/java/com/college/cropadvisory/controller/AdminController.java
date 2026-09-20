package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.AnalyticsResponse;
import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.RoleUpdateRequest;
import com.college.cropadvisory.dto.UserSummaryResponse;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.AdminService;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUsers() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", adminService.getAllUsers()));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUserRole(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RoleUpdateRequest request) {
        User admin = userService.getUserByEmail(userDetails.getUsername());
        UserSummaryResponse updated = adminService.updateUserRole(admin, userId, request.getRole());
        return ResponseEntity.ok(new ApiResponse<>(true, "User role updated", updated));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Object>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByEmail(userDetails.getUsername());
        adminService.deleteUser(admin, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Analytics fetched", adminService.getAnalytics()));
    }
}
