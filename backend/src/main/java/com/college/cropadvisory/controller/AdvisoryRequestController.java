package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.AdvisoryRequestRequest;
import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.model.entity.AdvisoryRequest;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.AdvisoryRequestService;
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
@RequestMapping("/api/advisory-requests")
public class AdvisoryRequestController {

    private final AdvisoryRequestService advisoryRequestService;
    private final UserService userService;

    public AdvisoryRequestController(AdvisoryRequestService advisoryRequestService,
                                     UserService userService) {
        this.advisoryRequestService = advisoryRequestService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<AdvisoryRequest>> submitRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AdvisoryRequestRequest request) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        AdvisoryRequest advisory = advisoryRequestService.submitRequest(
                farmer, request.getFarmId(), request.getQuestionText());
        return ResponseEntity.ok(new ApiResponse<>(true, "Advisory request submitted", advisory));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<List<AdvisoryRequest>>> getFarmerRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        User farmer = userService.getUserByEmail(userDetails.getUsername());
        List<AdvisoryRequest> list = advisoryRequestService.getRequestsForFarmer(farmer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Requests fetched", list));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<List<AdvisoryRequest>>> getOfficerQueue(
            @AuthenticationPrincipal UserDetails userDetails) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        List<AdvisoryRequest> list = advisoryRequestService.getRequestsForOfficer(officer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Queue fetched", list));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<AdvisoryRequest>> assignToMe(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        AdvisoryRequest updated = advisoryRequestService.assignToOfficer(id, officer);
        return ResponseEntity.ok(new ApiResponse<>(true, "Request assigned", updated));
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<AdvisoryRequest>> respond(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        String responseText = body.get("responseText");
        if (responseText == null || responseText.isBlank()) {
            throw new RuntimeException("responseText is required");
        }
        AdvisoryRequest updated = advisoryRequestService.respondToRequest(id, officer, responseText);
        return ResponseEntity.ok(new ApiResponse<>(true, "Response submitted", updated));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdvisoryRequest>> closeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        AdvisoryRequest updated = advisoryRequestService.closeRequest(id, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Request closed", updated));
    }
}