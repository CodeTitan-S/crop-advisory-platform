package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.DiseaseReportRequest;
import com.college.cropadvisory.dto.DiseaseResolutionRequest;
import com.college.cropadvisory.dto.ReassignRequest;
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
    @PreAuthorize("hasRole('OFFICER') or hasRole('ADMIN')")
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
            @Valid @RequestBody DiseaseResolutionRequest request) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        DiseaseReport report =
                diseaseReportService.resolveReport(id, officer, request.getResolutionNotes());
        return ResponseEntity.ok(new ApiResponse<>(true, "Resolved", report));
    }

    /** Admin escape hatch: move a stuck REPORTED/UNDER_REVIEW report to another officer. */
    @PutMapping("/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DiseaseReport>> reassignReport(
            @PathVariable Long id,
            @Valid @RequestBody ReassignRequest request) {
        User newOfficer = userService.getUserById(request.getOfficerId());
        DiseaseReport report = diseaseReportService.reassignReport(id, newOfficer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report reassigned", report));
    }

    /** Admin override: resolve an UNDER_REVIEW report when its officer is unreachable. */
    @PutMapping("/{id}/admin-resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DiseaseReport>> adminResolveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DiseaseResolutionRequest request) {
        User admin = userService.getUserByEmail(userDetails.getUsername());
        DiseaseReport report =
                diseaseReportService.resolveReportAsAdmin(id, admin, request.getResolutionNotes());
        return ResponseEntity.ok(new ApiResponse<>(true, "Resolved", report));
    }
}