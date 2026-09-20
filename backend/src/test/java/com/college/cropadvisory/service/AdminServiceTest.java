package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.AnalyticsResponse;
import com.college.cropadvisory.dto.UserSummaryResponse;
import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.AdvisoryRequest;
import com.college.cropadvisory.model.entity.AdvisoryStatus;
import com.college.cropadvisory.model.entity.DiseaseReport;
import com.college.cropadvisory.model.entity.DiseaseStatus;
import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.AdvisoryRequestRepository;
import com.college.cropadvisory.repository.DiseaseReportRepository;
import com.college.cropadvisory.repository.FarmRepository;
import com.college.cropadvisory.repository.KnowledgeBaseEntryRepository;
import com.college.cropadvisory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminService}.
 * Tests user listing and the guards that stop an admin locking themselves (or the platform) out,
 * plus the analytics aggregation.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private AdvisoryRequestRepository advisoryRequestRepository;

    @Mock
    private DiseaseReportRepository diseaseReportRepository;

    @Mock
    private KnowledgeBaseEntryRepository knowledgeBaseEntryRepository;

    @InjectMocks
    private AdminService adminService;

    private User admin;
    private User farmer;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setName("Platform Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        farmer = new User();
        farmer.setId(5L);
        farmer.setName("Jane Farmer");
        farmer.setEmail("jane@example.com");
        farmer.setRole(Role.FARMER);
    }

    // ─── getAllUsers ────────────────────────────────────────────────────

    /** Happy path: users are listed with their farm counts and never their password hash. */
    @Test
    @DisplayName("getAllUsers – success: returns users with farm counts")
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(admin, farmer));
        when(farmRepository.countByUser(admin)).thenReturn(0L);
        when(farmRepository.countByUser(farmer)).thenReturn(2L);

        List<UserSummaryResponse> result = adminService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("admin@example.com", result.get(0).getEmail());
        assertEquals("ADMIN", result.get(0).getRole());
        assertEquals(0L, result.get(0).getFarmCount());
        assertEquals("jane@example.com", result.get(1).getEmail());
        assertEquals(2L, result.get(1).getFarmCount());
    }

    // ─── updateUserRole ─────────────────────────────────────────────────

    /** Happy path: a farmer is promoted to officer. */
    @Test
    @DisplayName("updateUserRole – success: promotes a farmer to officer")
    void updateUserRole_success() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(farmer));
        when(userRepository.save(farmer)).thenReturn(farmer);
        when(farmRepository.countByUser(farmer)).thenReturn(2L);

        UserSummaryResponse result = adminService.updateUserRole(admin, 5L, "OFFICER");

        assertEquals("OFFICER", result.getRole());
        assertEquals(Role.OFFICER, farmer.getRole());
        verify(userRepository).save(farmer);
    }

    /** Edge case: re-applying the current role is a no-op rather than an error. */
    @Test
    @DisplayName("updateUserRole – edge: same role is a no-op")
    void updateUserRole_sameRole() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(farmer));
        when(farmRepository.countByUser(farmer)).thenReturn(2L);

        UserSummaryResponse result = adminService.updateUserRole(admin, 5L, "FARMER");

        assertEquals("FARMER", result.getRole());
    }

    /** Fail: an admin cannot change their own role, which could lock them out. */
    @Test
    @DisplayName("updateUserRole – fail: cannot change own role")
    void updateUserRole_self() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> adminService.updateUserRole(admin, 1L, "OFFICER"));

        assertEquals("You cannot change your own role", ex.getMessage());
    }

    /** Fail: demoting the only admin would leave the platform unmanageable. */
    @Test
    @DisplayName("updateUserRole – fail: cannot demote the last admin")
    void updateUserRole_lastAdmin() {
        User otherAdmin = new User();
        otherAdmin.setId(2L);
        otherAdmin.setEmail("other-admin@example.com");
        otherAdmin.setRole(Role.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> adminService.updateUserRole(admin, 2L, "FARMER"));

        assertEquals("Cannot change the role of the last remaining admin", ex.getMessage());
    }

    /** Fail: an unrecognised role name. */
    @Test
    @DisplayName("updateUserRole – fail: unknown role is a bad request")
    void updateUserRole_unknownRole() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(farmer));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> adminService.updateUserRole(admin, 5L, "SUPERUSER"));

        assertTrue(ex.getMessage().contains("SUPERUSER"));
    }

    /** Fail: unknown user id. */
    @Test
    @DisplayName("updateUserRole – fail: user not found")
    void updateUserRole_notFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> adminService.updateUserRole(admin, 999L, "OFFICER"));
    }

    // ─── deleteUser ─────────────────────────────────────────────────────

    /** Happy path: a user with no records is deleted. */
    @Test
    @DisplayName("deleteUser – success: deletes a user with no records")
    void deleteUser_success() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(farmer));

        adminService.deleteUser(admin, 5L);

        verify(userRepository).delete(farmer);
    }

    /** Fail: an admin cannot delete their own account. */
    @Test
    @DisplayName("deleteUser – fail: cannot delete own account")
    void deleteUser_self() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> adminService.deleteUser(admin, 1L));

        assertEquals("You cannot delete your own account", ex.getMessage());
    }

    /** Fail: deleting the only admin would leave the platform unmanageable. */
    @Test
    @DisplayName("deleteUser – fail: cannot delete the last admin")
    void deleteUser_lastAdmin() {
        User otherAdmin = new User();
        otherAdmin.setId(2L);
        otherAdmin.setEmail("other-admin@example.com");
        otherAdmin.setRole(Role.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> adminService.deleteUser(admin, 2L));
    }

    /** Fail: a user that still owns records would violate a foreign key. */
    @Test
    @DisplayName("deleteUser – fail: user with dependent records is refused")
    void deleteUser_hasDependents() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(farmer));
        when(farmRepository.countByUser(farmer)).thenReturn(2L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> adminService.deleteUser(admin, 5L));

        assertTrue(ex.getMessage().contains("farms or workflow records"));
    }

    // ─── getAnalytics ───────────────────────────────────────────────────

    /** Happy path: totals, per-week volumes and keyword-matched diseases are aggregated. */
    @Test
    @DisplayName("getAnalytics – success: aggregates counts, weeks and top diseases")
    void getAnalytics_success() {
        AdvisoryRequest pending = new AdvisoryRequest();
        pending.setStatus(AdvisoryStatus.PENDING);
        pending.setCreatedAt(LocalDateTime.now());

        AdvisoryRequest closed = new AdvisoryRequest();
        closed.setStatus(AdvisoryStatus.CLOSED);
        closed.setCreatedAt(LocalDateTime.now().minusWeeks(2));

        DiseaseReport blight = new DiseaseReport();
        blight.setStatus(DiseaseStatus.REPORTED);
        blight.setDescription("Leaves are yellowing with leaf blight patches");
        blight.setCreatedAt(LocalDateTime.now());

        KnowledgeBaseEntry entry = new KnowledgeBaseEntry();
        entry.setCropOrDiseaseName("Leaf Blight");

        KnowledgeBaseEntry unused = new KnowledgeBaseEntry();
        unused.setCropOrDiseaseName("Powdery Mildew");

        when(userRepository.findAll()).thenReturn(List.of(admin, farmer));
        when(advisoryRequestRepository.findAll()).thenReturn(List.of(pending, closed));
        when(diseaseReportRepository.findAll()).thenReturn(List.of(blight));
        when(farmRepository.count()).thenReturn(3L);
        when(knowledgeBaseEntryRepository.findAll()).thenReturn(List.of(entry, unused));

        AnalyticsResponse result = adminService.getAnalytics();

        assertEquals(2L, result.getTotalUsers());
        assertEquals(1L, result.getUsersByRole().get("ADMIN"));
        assertEquals(1L, result.getUsersByRole().get("FARMER"));
        assertEquals(3L, result.getTotalFarms());
        assertEquals(2L, result.getTotalAdvisoryRequests());
        assertEquals(1L, result.getRequestsByStatus().get("PENDING"));
        assertEquals(1L, result.getRequestsByStatus().get("CLOSED"));
        assertEquals(1L, result.getTotalDiseaseReports());

        // Eight ISO weeks, oldest first, with this week's request counted.
        assertEquals(8, result.getRequestsPerWeek().size());
        assertEquals(1L, result.getRequestsPerWeek().get(7).getCount());

        // Only the knowledge-base name actually mentioned in a description is ranked.
        assertEquals(1, result.getTopDiseases().size());
        assertEquals("Leaf Blight", result.getTopDiseases().get(0).getName());
        assertEquals(1L, result.getTopDiseases().get(0).getCount());
    }

    /** Edge case: an empty knowledge base yields no disease ranking rather than an error. */
    @Test
    @DisplayName("getAnalytics – edge: empty knowledge base yields no top diseases")
    void getAnalytics_noKnowledgeBase() {
        DiseaseReport report = new DiseaseReport();
        report.setStatus(DiseaseStatus.REPORTED);
        report.setDescription("Something is wrong with the crop");
        report.setCreatedAt(LocalDateTime.now());

        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(advisoryRequestRepository.findAll()).thenReturn(Collections.emptyList());
        when(diseaseReportRepository.findAll()).thenReturn(List.of(report));
        when(knowledgeBaseEntryRepository.findAll()).thenReturn(Collections.emptyList());

        AnalyticsResponse result = adminService.getAnalytics();

        assertTrue(result.getTopDiseases().isEmpty());
        assertEquals(0L, result.getTotalAdvisoryRequests());
        assertEquals(8, result.getRequestsPerWeek().size());
    }
}
