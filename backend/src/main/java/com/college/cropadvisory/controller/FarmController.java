package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.FarmRequest;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.FarmService;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmService farmService;
    private final UserService userService;

    public FarmController(FarmService farmService, UserService userService) {
        this.farmService = farmService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Farm>> createFarm(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FarmRequest request) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        Farm farm = farmService.createFarm(farmer, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Farm created", farm));
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<List<Farm>>> getMyFarms(
            @AuthenticationPrincipal UserDetails userDetails) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        List<Farm> farms = farmService.getFarmsByUser(farmer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Farms fetched", farms));
    }
}