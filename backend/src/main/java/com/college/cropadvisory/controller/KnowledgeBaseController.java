package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.dto.KnowledgeBaseEntryRequest;
import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.KnowledgeBaseService;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Knowledge base curation (create/update/delete) is Admin-only, per the Problem Statement.
 * Reads are open to OFFICER as well: officers are the ones advising farmers, so denying them
 * the reference material made the feature useless to its primary audience.
 */
@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final UserService userService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   UserService userService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseEntry>>> getEntries() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Knowledge base entries fetched", knowledgeBaseService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<KnowledgeBaseEntry>> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Knowledge base entry fetched", knowledgeBaseService.getEntry(id)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OFFICER')")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseEntry>>> searchEntries(@RequestParam String q) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Knowledge base entries searched", knowledgeBaseService.search(q)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KnowledgeBaseEntry>> createEntry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody KnowledgeBaseEntryRequest request) {
        User admin = userService.getUserByEmail(userDetails.getUsername());
        KnowledgeBaseEntry entry = knowledgeBaseService.create(admin, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Knowledge base entry created", entry));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KnowledgeBaseEntry>> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseEntryRequest request) {
        KnowledgeBaseEntry entry = knowledgeBaseService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Knowledge base entry updated", entry));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteEntry(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Knowledge base entry deleted", null));
    }
}
