package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.SoilReadingRequest;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SoilReading;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.FarmRepository;
import com.college.cropadvisory.service.SoilReadingService;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms/{farmId}/soil-readings")
public class SoilReadingController {

    private final SoilReadingService soilReadingService;
    private final FarmRepository farmRepository;
    private final UserService userService;

    public SoilReadingController(SoilReadingService soilReadingService,
                                 FarmRepository farmRepository,
                                 UserService userService) {
        this.soilReadingService = soilReadingService;
        this.farmRepository = farmRepository;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<SoilReading>> logReading(
            @PathVariable Long farmId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SoilReadingRequest request) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));
        if (!farm.getUser().getId().equals(farmer.getId())) {
            throw new RuntimeException("Access denied");
        }
        SoilReading reading = soilReadingService.logReading(farm, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Soil reading logged", reading));
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER') or hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<List<SoilReading>>> getReadings(
            @PathVariable Long farmId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));
        // simple check: farmer owns farm, or officer can view
        List<SoilReading> readings = soilReadingService.getReadingsByFarm(farm);
        return ResponseEntity.ok(new ApiResponse<>(true, "Readings fetched", readings));
    }
}