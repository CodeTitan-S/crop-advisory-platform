package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.DiseaseReportRequest;
import com.college.cropadvisory.model.entity.DiseaseReport;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.DiseaseReportService;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disease-reports")
public class DiseaseReportController {

    private final DiseaseReportService diseaseReportService;
    private final UserService userService;

    public DiseaseReportController(DiseaseReportService diseaseReportService,
                                   UserService userService) {
        this.diseaseReportService = diseaseReportService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<DiseaseReport>> submitReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DiseaseReportRequest request) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        DiseaseReport report = diseaseReportService.submitReport(farmer, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report submitted", report));
    }

    @GetMapping("/my-reports")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<List<DiseaseReport>>> getFarmerReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        List<DiseaseReport> list = diseaseReportService.getReportsForFarmer(farmer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reports fetched", list));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<List<DiseaseReport>>> getOfficerQueue(
            @AuthenticationPrincipal UserDetails userDetails) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        List<DiseaseReport> list = diseaseReportService.getReportsForOfficer(officer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Queue fetched", list));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<DiseaseReport>> reviewReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        DiseaseReport report = diseaseReportService.reviewReport(id, officer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Under review", report));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<DiseaseReport>> resolveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        String resolutionNotes = body.get("resolutionNotes");
        DiseaseReport report = diseaseReportService.resolveReport(id, officer, resolutionNotes);
        return ResponseEntity.ok(new ApiResponse<>(true, "Resolved", report));
    }
}