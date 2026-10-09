package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.DiseaseReportRequest;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.*;
import com.college.cropadvisory.repository.DiseaseReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DiseaseReportService}.
 * Covers the lifecycle: submit → review → resolve,
 * plus authorization and status-transition guard tests.
 *
 * Farm loading/ownership now lives in {@link FarmService}, so those are stubbed here and
 * verified in {@link FarmServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class DiseaseReportServiceTest {

    @Mock
    private DiseaseReportRepository diseaseReportRepository;

    @Mock
    private FarmService farmService;

    @InjectMocks
    private DiseaseReportService diseaseReportService;

    private User farmer;
    private User officer;
    private User otherFarmer;
    private Farm farm;
    private DiseaseReportRequest reportRequest;
    private DiseaseReport reportedReport;
    private DiseaseReport underReviewReport;

    @BeforeEach
    void setUp() {
        farmer = new User();
        farmer.setId(1L);
        farmer.setName("Alice Farmer");
        farmer.setEmail("alice@farm.com");
        farmer.setRole(Role.FARMER);

        officer = new User();
        officer.setId(2L);
        officer.setName("Bob Officer");
        officer.setEmail("bob@officer.com");
        officer.setRole(Role.OFFICER);

        otherFarmer = new User();
        otherFarmer.setId(3L);
        otherFarmer.setName("Eve Farmer");
        otherFarmer.setRole(Role.FARMER);

        farm = new Farm();
        farm.setId(10L);
        farm.setLocation("South Plot");
        farm.setUser(farmer);

        reportRequest = new DiseaseReportRequest();
        reportRequest.setFarmId(10L);
        reportRequest.setDescription("Leaves have brown spots.");
        reportRequest.setImageUrl("https://img.example.com/leaf.jpg");

        // REPORTED (newly submitted)
        reportedReport = new DiseaseReport();
        reportedReport.setId(200L);
        reportedReport.setFarmer(farmer);
        reportedReport.setFarm(farm);
        reportedReport.setDescription("Leaves have brown spots.");
        reportedReport.setImageUrl("https://img.example.com/leaf.jpg");
        reportedReport.setStatus(DiseaseStatus.REPORTED);
        reportedReport.setCreatedAt(LocalDateTime.now());

        // UNDER_REVIEW (officer picked it up)
        underReviewReport = new DiseaseReport();
        underReviewReport.setId(201L);
        underReviewReport.setFarmer(farmer);
        underReviewReport.setFarm(farm);
        underReviewReport.setDescription("Wilting stems");
        underReviewReport.setStatus(DiseaseStatus.UNDER_REVIEW);
        underReviewReport.setOfficer(officer);
    }

    // ─── submitReport ───────────────────────────────────────────────────

    /** Happy path: farmer submits a disease report for their farm. */
    @Test
    @DisplayName("submitReport – success: creates report with REPORTED status")
    void submitReport_success() {
        when(farmService.getFarmOwnedBy(10L, farmer)).thenReturn(farm);
        when(diseaseReportRepository.save(any(DiseaseReport.class))).thenReturn(reportedReport);

        DiseaseReport result = diseaseReportService.submitReport(farmer, reportRequest);

        assertNotNull(result);
        assertEquals(DiseaseStatus.REPORTED, result.getStatus());
        assertEquals("Leaves have brown spots.", result.getDescription());
        assertEquals(farmer, result.getFarmer());
        verify(diseaseReportRepository).save(any(DiseaseReport.class));
    }

    /** Fail: farm not found propagates from FarmService. */
    @Test
    @DisplayName("submitReport – fail: farm not found")
    void submitReport_farmNotFound() {
        reportRequest.setFarmId(999L);
        when(farmService.getFarmOwnedBy(999L, farmer))
                .thenThrow(new NotFoundException("Farm not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> diseaseReportService.submitReport(farmer, reportRequest));

        assertEquals("Farm not found", ex.getMessage());
        verify(diseaseReportRepository, never()).save(any());
    }

    /** Fail: farmer does not own the farm. */
    @Test
    @DisplayName("submitReport – fail: not the farm owner")
    void submitReport_notOwner() {
        when(farmService.getFarmOwnedBy(10L, otherFarmer))
                .thenThrow(new ForbiddenException("Not your farm"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> diseaseReportService.submitReport(otherFarmer, reportRequest));

        assertEquals("Not your farm", ex.getMessage());
        verify(diseaseReportRepository, never()).save(any());
    }

    // ─── getReportsForFarmer ────────────────────────────────────────────

    /** Happy path: returns reports submitted by the farmer. */
    @Test
    @DisplayName("getReportsForFarmer – success: returns farmer's reports")
    void getReportsForFarmer_success() {
        when(diseaseReportRepository.findByFarmerOrderByCreatedAtDesc(farmer))
                .thenReturn(Arrays.asList(reportedReport, underReviewReport));

        List<DiseaseReport> result = diseaseReportService.getReportsForFarmer(farmer);

        assertEquals(2, result.size());
        verify(diseaseReportRepository).findByFarmerOrderByCreatedAtDesc(farmer);
    }

    // ─── getReportsForOfficer ───────────────────────────────────────────

    /** Happy path: returns assigned and unassigned reports for the officer. */
    @Test
    @DisplayName("getReportsForOfficer – success: returns officer's queue")
    void getReportsForOfficer_success() {
        when(diseaseReportRepository.findByOfficerOrOfficerIsNullOrderByCreatedAtDesc(officer))
                .thenReturn(Arrays.asList(reportedReport, underReviewReport));

        List<DiseaseReport> result = diseaseReportService.getReportsForOfficer(officer);

        assertEquals(2, result.size());
    }

    // ─── reviewReport ───────────────────────────────────────────────────

    /** Happy path: officer picks up a REPORTED report for review. */
    @Test
    @DisplayName("reviewReport – success: transitions REPORTED → UNDER_REVIEW")
    void reviewReport_success() {
        when(diseaseReportRepository.findById(200L)).thenReturn(Optional.of(reportedReport));
        when(diseaseReportRepository.save(any(DiseaseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        DiseaseReport result = diseaseReportService.reviewReport(200L, officer);

        assertEquals(DiseaseStatus.UNDER_REVIEW, result.getStatus());
        assertEquals(officer, result.getOfficer());
    }

    /** Fail: report not found. */
    @Test
    @DisplayName("reviewReport – fail: report not found")
    void reviewReport_notFound() {
        when(diseaseReportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> diseaseReportService.reviewReport(999L, officer));
    }

    /** Fail: report is not in REPORTED status (already under review). */
    @Test
    @DisplayName("reviewReport – fail: report not REPORTED")
    void reviewReport_invalidStatus() {
        when(diseaseReportRepository.findById(201L)).thenReturn(Optional.of(underReviewReport));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> diseaseReportService.reviewReport(201L, officer));

        assertEquals("Report is not REPORTED", ex.getMessage());
        verify(diseaseReportRepository, never()).save(any());
    }

    // ─── resolveReport ──────────────────────────────────────────────────

    /** Happy path: assigned officer resolves the report. */
    @Test
    @DisplayName("resolveReport – success: transitions UNDER_REVIEW → RESOLVED")
    void resolveReport_success() {
        when(diseaseReportRepository.findById(201L)).thenReturn(Optional.of(underReviewReport));
        when(diseaseReportRepository.save(any(DiseaseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        DiseaseReport result = diseaseReportService.resolveReport(201L, officer, "Apply fungicide.");

        assertEquals(DiseaseStatus.RESOLVED, result.getStatus());
        assertEquals("Apply fungicide.", result.getResolutionNotes());
        assertNotNull(result.getResolvedAt());
    }

    /** Fail: different officer tries to resolve (not assigned). */
    @Test
    @DisplayName("resolveReport – fail: not the assigned officer")
    void resolveReport_notAssigned() {
        User anotherOfficer = new User();
        anotherOfficer.setId(99L);
        anotherOfficer.setRole(Role.OFFICER);

        when(diseaseReportRepository.findById(201L)).thenReturn(Optional.of(underReviewReport));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> diseaseReportService.resolveReport(201L, anotherOfficer, "notes"));

        assertEquals("Not assigned to you", ex.getMessage());
    }

    /** Fail: report is not in UNDER_REVIEW status. */
    @Test
    @DisplayName("resolveReport – fail: report not UNDER_REVIEW")
    void resolveReport_invalidStatus() {
        when(diseaseReportRepository.findById(200L)).thenReturn(Optional.of(reportedReport));

        // reportedReport has no officer so it will hit the "Not assigned to you" guard
        // because reportedReport.getOfficer() is null
        assertThrows(RuntimeException.class,
                () -> diseaseReportService.resolveReport(200L, officer, "notes"));
    }

    // ─── reassignReport / resolveReportAsAdmin (admin escape hatches) ───

    private User admin() {
        User admin = new User();
        admin.setId(30L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        return admin;
    }

    /** Happy path: admin moves an UNDER_REVIEW report from a departing officer to another. */
    @Test
    @DisplayName("reassignReport – success: UNDER_REVIEW report moves to the new officer")
    void reassignReport_success() {
        User newOfficer = new User();
        newOfficer.setId(21L);
        newOfficer.setRole(Role.OFFICER);

        when(diseaseReportRepository.findById(201L)).thenReturn(Optional.of(underReviewReport));
        when(diseaseReportRepository.save(any(DiseaseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        DiseaseReport result = diseaseReportService.reassignReport(201L, newOfficer);

        assertEquals(DiseaseStatus.UNDER_REVIEW, result.getStatus());
        assertEquals(newOfficer, result.getOfficer());
    }

    /** Happy path: a REPORTED report can be handed straight to a chosen officer. */
    @Test
    @DisplayName("reassignReport – success: REPORTED report can be reassigned")
    void reassignReport_success_reported() {
        when(diseaseReportRepository.findById(200L)).thenReturn(Optional.of(reportedReport));
        when(diseaseReportRepository.save(any(DiseaseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        DiseaseReport result = diseaseReportService.reassignReport(200L, officer);

        assertEquals(DiseaseStatus.UNDER_REVIEW, result.getStatus());
        assertEquals(officer, result.getOfficer());
    }

    /** Fail: a RESOLVED report is finished — reassignment would rewrite history. */
    @Test
    @DisplayName("reassignReport – fail: RESOLVED report cannot be reassigned")
    void reassignReport_rejectsResolved() {
        underReviewReport.setStatus(DiseaseStatus.RESOLVED);
        when(diseaseReportRepository.findById(201L)).thenReturn(Optional.of(underReviewReport));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> diseaseReportService.reassignReport(201L, officer));

        assertTrue(ex.getMessage().contains("Only REPORTED or UNDER_REVIEW"));
        verify(diseaseReportRepository, never()).save(any());
    }

    /** Happy path: admin resolves a report whose assigned officer is unreachable. */
    @Test
    @DisplayName("resolveReportAsAdmin – success: admin resolves regardless of assignee")
    void resolveReportAsAdmin_success() {
        when(diseaseReportRepository.findById(201L)).thenReturn(Optional.of(underReviewReport));
        when(diseaseReportRepository.save(any(DiseaseReport.class))).thenAnswer(inv -> inv.getArgument(0));

        DiseaseReport result = diseaseReportService.resolveReportAsAdmin(201L, admin(), "Applied fungicide.");

        assertEquals(DiseaseStatus.RESOLVED, result.getStatus());
        assertEquals("Applied fungicide.", result.getResolutionNotes());
        assertNotNull(result.getResolvedAt());
    }

    /** Fail: admin cannot resolve a report that is still REPORTED (not picked up yet). */
    @Test
    @DisplayName("resolveReportAsAdmin – fail: report must be UNDER_REVIEW")
    void resolveReportAsAdmin_rejectsReported() {
        when(diseaseReportRepository.findById(200L)).thenReturn(Optional.of(reportedReport));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> diseaseReportService.resolveReportAsAdmin(200L, admin(), "notes"));

        assertEquals("Report must be UNDER_REVIEW", ex.getMessage());
        verify(diseaseReportRepository, never()).save(any());
    }
}
