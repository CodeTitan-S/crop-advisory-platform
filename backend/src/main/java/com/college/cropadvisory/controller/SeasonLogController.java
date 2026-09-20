package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.SeasonLogRequest;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SeasonLog;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.FarmService;
import com.college.cropadvisory.service.SeasonLogService;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms/{farmId}/season-logs")
public class SeasonLogController {

    private final SeasonLogService seasonLogService;
    private final FarmService farmService;
    private final UserService userService;

    public SeasonLogController(SeasonLogService seasonLogService,
                               FarmService farmService,
                               UserService userService) {
        this.seasonLogService = seasonLogService;
        this.farmService = farmService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<SeasonLog>> logSeason(
            @PathVariable Long farmId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SeasonLogRequest request) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        Farm farm = farmService.getFarmOwnedBy(farmId, farmer);
        SeasonLog log = seasonLogService.logSeason(farm, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Season log recorded", log));
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER') or hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<List<SeasonLog>>> getLogs(
            @PathVariable Long farmId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        Farm farm = farmService.getFarmReadableBy(farmId, user);
        List<SeasonLog> logs = seasonLogService.getLogsByFarm(farm);
        return ResponseEntity.ok(new ApiResponse<>(true, "Season logs fetched", logs));
    }

    @GetMapping("/{logId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<SeasonLog>> getLog(
            @PathVariable Long farmId,
            @PathVariable Long logId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        SeasonLog log = seasonLogService.getLogForFarm(farmId, logId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Season log fetched", log));
    }

    @PutMapping("/{logId}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<SeasonLog>> updateLog(
            @PathVariable Long farmId,
            @PathVariable Long logId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SeasonLogRequest request) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        SeasonLog updated = seasonLogService.updateLog(farmId, logId, farmer, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Season log updated", updated));
    }

    @DeleteMapping("/{logId}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Object>> deleteLog(
            @PathVariable Long farmId,
            @PathVariable Long logId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        seasonLogService.deleteLog(farmId, logId, farmer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Season log deleted", null));
    }
}
