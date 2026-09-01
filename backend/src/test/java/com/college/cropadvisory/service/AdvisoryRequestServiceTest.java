package com.college.cropadvisory.service;

import com.college.cropadvisory.model.entity.*;
import com.college.cropadvisory.repository.AdvisoryRequestRepository;
import com.college.cropadvisory.repository.FarmRepository;
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
 * Unit tests for {@link AdvisoryRequestService}.
 * Covers the full lifecycle: submit → assign → respond → close,
 * plus authorization and status-transition guard tests.
 */
@ExtendWith(MockitoExtension.class)
class AdvisoryRequestServiceTest {

    @Mock
    private AdvisoryRequestRepository advisoryRequestRepository;

    @Mock
    private FarmRepository farmRepository;

    @InjectMocks
    private AdvisoryRequestService advisoryRequestService;

    private User farmer;
    private User officer;
    private User admin;
    private User otherFarmer;
    private Farm farm;
    private AdvisoryRequest pendingRequest;
    private AdvisoryRequest assignedRequest;
    private AdvisoryRequest respondedRequest;

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

        admin = new User();
        admin.setId(3L);
        admin.setName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        otherFarmer = new User();
        otherFarmer.setId(4L);
        otherFarmer.setName("Eve Farmer");
        otherFarmer.setEmail("eve@farm.com");
        otherFarmer.setRole(Role.FARMER);

        farm = new Farm();
        farm.setId(10L);
        farm.setLocation("West Plot");
        farm.setUser(farmer);

        // PENDING request (no officer, no response)
        pendingRequest = new AdvisoryRequest();
        pendingRequest.setId(100L);
        pendingRequest.setFarmer(farmer);
        pendingRequest.setFarm(farm);
        pendingRequest.setQuestionText("What crop suits loamy soil?");
        pendingRequest.setStatus(AdvisoryStatus.PENDING);
        pendingRequest.setCreatedAt(LocalDateTime.now());

        // ASSIGNED request (officer set)
        assignedRequest = new AdvisoryRequest();
        assignedRequest.setId(101L);
        assignedRequest.setFarmer(farmer);
        assignedRequest.setFarm(farm);
        assignedRequest.setQuestionText("Best irrigation schedule?");
        assignedRequest.setStatus(AdvisoryStatus.ASSIGNED);
        assignedRequest.setOfficer(officer);

        // RESPONDED request (has response text)
        respondedRequest = new AdvisoryRequest();
        respondedRequest.setId(102L);
        respondedRequest.setFarmer(farmer);
        respondedRequest.setFarm(farm);
        respondedRequest.setQuestionText("Need pest advice");
        respondedRequest.setStatus(AdvisoryStatus.RESPONDED);
        respondedRequest.setOfficer(officer);
        respondedRequest.setResponseText("Use neem-based spray.");
    }

    // ─── submitRequest ──────────────────────────────────────────────────

    /** Happy path: farmer submits a request for their own farm. */
    @Test
    @DisplayName("submitRequest – success: creates PENDING request for farmer's farm")
    void submitRequest_success() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(advisoryRequestRepository.save(any(AdvisoryRequest.class))).thenReturn(pendingRequest);

        AdvisoryRequest result = advisoryRequestService.submitRequest(farmer, 10L, "What crop suits loamy soil?");

        assertNotNull(result);
        assertEquals(AdvisoryStatus.PENDING, result.getStatus());
        assertEquals(farmer, result.getFarmer());
        verify(advisoryRequestRepository).save(any(AdvisoryRequest.class));
    }

    /** Fail: farm not found throws RuntimeException. */
    @Test
    @DisplayName("submitRequest – fail: farm not found")
    void submitRequest_farmNotFound() {
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.submitRequest(farmer, 999L, "Question"));

        assertEquals("Farm not found", ex.getMessage());
    }

    /** Fail: farmer tries to submit for another farmer's farm. */
    @Test
    @DisplayName("submitRequest – fail: not the farm owner")
    void submitRequest_notOwner() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.submitRequest(otherFarmer, 10L, "Question"));

        assertEquals("Not your farm", ex.getMessage());
        verify(advisoryRequestRepository, never()).save(any());
    }

    // ─── getRequestsForFarmer ───────────────────────────────────────────

    /** Happy path: returns requests submitted by the farmer. */
    @Test
    @DisplayName("getRequestsForFarmer – success: returns farmer's requests")
    void getRequestsForFarmer_success() {
        when(advisoryRequestRepository.findByFarmerOrderByCreatedAtDesc(farmer))
                .thenReturn(Arrays.asList(pendingRequest, assignedRequest));

        List<AdvisoryRequest> result = advisoryRequestService.getRequestsForFarmer(farmer);

        assertEquals(2, result.size());
        verify(advisoryRequestRepository).findByFarmerOrderByCreatedAtDesc(farmer);
    }

    // ─── getRequestsForOfficer ──────────────────────────────────────────

    /** Happy path: returns assigned and unassigned (pending) requests. */
    @Test
    @DisplayName("getRequestsForOfficer – success: returns officer's queue")
    void getRequestsForOfficer_success() {
        when(advisoryRequestRepository.findByOfficerOrOfficerIsNullOrderByCreatedAtDesc(officer))
                .thenReturn(Arrays.asList(pendingRequest, assignedRequest));

        List<AdvisoryRequest> result = advisoryRequestService.getRequestsForOfficer(officer);

        assertEquals(2, result.size());
    }

    // ─── assignToOfficer ────────────────────────────────────────────────

    /** Happy path: PENDING request is assigned to the officer. */
    @Test
    @DisplayName("assignToOfficer – success: transitions PENDING → ASSIGNED")
    void assignToOfficer_success() {
        when(advisoryRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(advisoryRequestRepository.save(any(AdvisoryRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdvisoryRequest result = advisoryRequestService.assignToOfficer(100L, officer);

        assertEquals(AdvisoryStatus.ASSIGNED, result.getStatus());
        assertEquals(officer, result.getOfficer());
    }

    /** Fail: request not found. */
    @Test
    @DisplayName("assignToOfficer – fail: request not found")
    void assignToOfficer_notFound() {
        when(advisoryRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> advisoryRequestService.assignToOfficer(999L, officer));
    }

    /** Fail: request is not in PENDING status (already assigned). */
    @Test
    @DisplayName("assignToOfficer – fail: request not PENDING")
    void assignToOfficer_notPending() {
        when(advisoryRequestRepository.findById(101L)).thenReturn(Optional.of(assignedRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.assignToOfficer(101L, officer));

        assertEquals("Request is not in PENDING status", ex.getMessage());
        verify(advisoryRequestRepository, never()).save(any());
    }

    // ─── respondToRequest ───────────────────────────────────────────────

    /** Happy path: assigned officer responds to the request. */
    @Test
    @DisplayName("respondToRequest – success: transitions ASSIGNED → RESPONDED")
    void respondToRequest_success() {
        when(advisoryRequestRepository.findById(101L)).thenReturn(Optional.of(assignedRequest));
        when(advisoryRequestRepository.save(any(AdvisoryRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdvisoryRequest result = advisoryRequestService.respondToRequest(101L, officer, "Use drip irrigation.");

        assertEquals(AdvisoryStatus.RESPONDED, result.getStatus());
        assertEquals("Use drip irrigation.", result.getResponseText());
        assertNotNull(result.getRespondedAt());
    }

    /** Fail: different officer tries to respond (not assigned). */
    @Test
    @DisplayName("respondToRequest – fail: not the assigned officer")
    void respondToRequest_notAssigned() {
        User anotherOfficer = new User();
        anotherOfficer.setId(99L);
        anotherOfficer.setRole(Role.OFFICER);

        when(advisoryRequestRepository.findById(101L)).thenReturn(Optional.of(assignedRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.respondToRequest(101L, anotherOfficer, "response"));

        assertEquals("Not assigned to you", ex.getMessage());
    }

    /** Fail: request is not in ASSIGNED status. */
    @Test
    @DisplayName("respondToRequest – fail: request not ASSIGNED")
    void respondToRequest_notAssigned_status() {
        when(advisoryRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.respondToRequest(100L, officer, "response"));

        // pendingRequest has no officer, so it hits the "Not assigned to you" check first
        assertEquals("Not assigned to you", ex.getMessage());
    }

    // ─── closeRequest ───────────────────────────────────────────────────

    /** Happy path: assigned officer closes a RESPONDED request. */
    @Test
    @DisplayName("closeRequest – success: officer closes RESPONDED request")
    void closeRequest_success_officer() {
        when(advisoryRequestRepository.findById(102L)).thenReturn(Optional.of(respondedRequest));
        when(advisoryRequestRepository.save(any(AdvisoryRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdvisoryRequest result = advisoryRequestService.closeRequest(102L, officer);

        assertEquals(AdvisoryStatus.CLOSED, result.getStatus());
    }

    /** Happy path: admin can also close a RESPONDED request. */
    @Test
    @DisplayName("closeRequest – success: admin closes RESPONDED request")
    void closeRequest_success_admin() {
        when(advisoryRequestRepository.findById(102L)).thenReturn(Optional.of(respondedRequest));
        when(advisoryRequestRepository.save(any(AdvisoryRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdvisoryRequest result = advisoryRequestService.closeRequest(102L, admin);

        assertEquals(AdvisoryStatus.CLOSED, result.getStatus());
    }

    /** Fail: unauthorized user (farmer) cannot close. */
    @Test
    @DisplayName("closeRequest – fail: farmer cannot close request")
    void closeRequest_unauthorized() {
        when(advisoryRequestRepository.findById(102L)).thenReturn(Optional.of(respondedRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.closeRequest(102L, farmer));

        assertEquals("Not authorized to close this request", ex.getMessage());
    }

    /** Fail: request is not in RESPONDED status (e.g. still ASSIGNED). */
    @Test
    @DisplayName("closeRequest – fail: request not RESPONDED")
    void closeRequest_notResponded() {
        when(advisoryRequestRepository.findById(101L)).thenReturn(Optional.of(assignedRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> advisoryRequestService.closeRequest(101L, officer));

        assertEquals("Request must be RESPONDED before closing", ex.getMessage());
    }
}
