package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.SeasonLogRequest;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.SeasonLog;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.SeasonLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SeasonLogService}.
 * Tests recording, listing, updating and deleting season logs, plus farm-ownership enforcement.
 */
@ExtendWith(MockitoExtension.class)
class SeasonLogServiceTest {

    private static final Long FARM_ID = 10L;
    private static final Long LOG_ID = 100L;

    @Mock
    private SeasonLogRepository seasonLogRepository;

    @Mock
    private FarmService farmService;

    @InjectMocks
    private SeasonLogService seasonLogService;

    private User farmer;
    private Farm farm;
    private SeasonLogRequest logRequest;
    private SeasonLog sampleLog;

    @BeforeEach
    void setUp() {
        farmer = new User();
        farmer.setId(1L);
        farmer.setEmail("jane@example.com");
        farmer.setRole(Role.FARMER);

        farm = new Farm();
        farm.setId(FARM_ID);
        farm.setLocation("North Valley");
        farm.setSoilType("Loamy");
        farm.setUser(farmer);

        logRequest = new SeasonLogRequest();
        logRequest.setCropPlanted("Wheat");
        logRequest.setSowingDate(LocalDate.of(2026, 6, 15));
        logRequest.setOutcomeNotes("Good yield, no disease issues");

        sampleLog = new SeasonLog();
        sampleLog.setId(LOG_ID);
        sampleLog.setFarm(farm);
        sampleLog.setCropPlanted("Wheat");
        sampleLog.setSowingDate(LocalDate.of(2026, 6, 15));
        sampleLog.setOutcomeNotes("Good yield, no disease issues");
    }

    // ─── logSeason ──────────────────────────────────────────────────────

    /** Happy path: the log is saved with the requested values and linked to the farm. */
    @Test
    @DisplayName("logSeason – success: saves log with correct fields")
    void logSeason_success() {
        when(seasonLogRepository.save(any(SeasonLog.class))).thenReturn(sampleLog);

        SeasonLog result = seasonLogService.logSeason(farm, logRequest);

        assertNotNull(result);
        assertEquals("Wheat", result.getCropPlanted());
        assertEquals(LocalDate.of(2026, 6, 15), result.getSowingDate());
        assertEquals("Good yield, no disease issues", result.getOutcomeNotes());
        assertEquals(farm, result.getFarm());
        assertEquals(FARM_ID, result.getFarmId());
        verify(seasonLogRepository).save(any(SeasonLog.class));
    }

    // ─── getLogsByFarm ──────────────────────────────────────────────────

    /** Happy path: returns the farm's logs newest-first. */
    @Test
    @DisplayName("getLogsByFarm – success: returns logs for farm")
    void getLogsByFarm_success() {
        SeasonLog older = new SeasonLog();
        older.setId(99L);
        older.setFarm(farm);
        older.setCropPlanted("Maize");
        older.setSowingDate(LocalDate.of(2025, 6, 1));

        when(seasonLogRepository.findByFarmOrderBySowingDateDesc(farm))
                .thenReturn(Arrays.asList(sampleLog, older));

        List<SeasonLog> result = seasonLogService.getLogsByFarm(farm);

        assertEquals(2, result.size());
        assertEquals(sampleLog, result.get(0));
        assertEquals("Maize", result.get(1).getCropPlanted());
        verify(seasonLogRepository).findByFarmOrderBySowingDateDesc(farm);
    }

    /** Edge case: a farm with no recorded seasons returns an empty list. */
    @Test
    @DisplayName("getLogsByFarm – edge: no logs returns empty list")
    void getLogsByFarm_empty() {
        when(seasonLogRepository.findByFarmOrderBySowingDateDesc(farm))
                .thenReturn(Collections.emptyList());

        List<SeasonLog> result = seasonLogService.getLogsByFarm(farm);

        assertTrue(result.isEmpty());
    }

    // ─── getLogForFarm ──────────────────────────────────────────────────

    /** Happy path: an owner can read a log on their own farm. */
    @Test
    @DisplayName("getLogForFarm – success: owner reads log on own farm")
    void getLogForFarm_success() {
        when(farmService.getFarmReadableBy(FARM_ID, farmer)).thenReturn(farm);
        when(seasonLogRepository.findById(LOG_ID)).thenReturn(Optional.of(sampleLog));

        SeasonLog result = seasonLogService.getLogForFarm(FARM_ID, LOG_ID, farmer);

        assertEquals(sampleLog, result);
    }

    /** Fail: unknown log id. */
    @Test
    @DisplayName("getLogForFarm – fail: log not found")
    void getLogForFarm_notFound() {
        when(farmService.getFarmReadableBy(FARM_ID, farmer)).thenReturn(farm);
        when(seasonLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> seasonLogService.getLogForFarm(FARM_ID, 999L, farmer));
    }

    /**
     * Fail: a log belonging to another farm is not reachable by pairing an owned farm id
     * with a foreign log id.
     */
    @Test
    @DisplayName("getLogForFarm – fail: log under a different farm is hidden")
    void getLogForFarm_wrongFarm() {
        Farm otherFarm = new Farm();
        otherFarm.setId(20L);
        otherFarm.setLocation("South Fields");

        SeasonLog foreignLog = new SeasonLog();
        foreignLog.setId(500L);
        foreignLog.setFarm(otherFarm);

        when(farmService.getFarmReadableBy(FARM_ID, farmer)).thenReturn(farm);
        when(seasonLogRepository.findById(500L)).thenReturn(Optional.of(foreignLog));

        assertThrows(NotFoundException.class,
                () -> seasonLogService.getLogForFarm(FARM_ID, 500L, farmer));
    }

    // ─── updateLog ──────────────────────────────────────────────────────

    /** Happy path: an owner can update their own log. */
    @Test
    @DisplayName("updateLog – success: owner updates log fields")
    void updateLog_success() {
        SeasonLogRequest update = new SeasonLogRequest();
        update.setCropPlanted("Barley");
        update.setSowingDate(LocalDate.of(2026, 7, 1));
        update.setOutcomeNotes("Partial harvest");

        when(farmService.getFarmOwnedBy(FARM_ID, farmer)).thenReturn(farm);
        when(seasonLogRepository.findById(LOG_ID)).thenReturn(Optional.of(sampleLog));
        when(seasonLogRepository.save(sampleLog)).thenReturn(sampleLog);

        SeasonLog result = seasonLogService.updateLog(FARM_ID, LOG_ID, farmer, update);

        assertEquals("Barley", result.getCropPlanted());
        assertEquals(LocalDate.of(2026, 7, 1), result.getSowingDate());
        assertEquals("Partial harvest", result.getOutcomeNotes());
        verify(seasonLogRepository).save(sampleLog);
    }

    /** Fail: updating an unknown log id. */
    @Test
    @DisplayName("updateLog – fail: log not found")
    void updateLog_notFound() {
        when(seasonLogRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> seasonLogService.updateLog(FARM_ID, 999L, farmer, logRequest));

        assertEquals("Season log not found", ex.getMessage());
    }

    /** Fail: a farmer cannot update a log on someone else's farm. */
    @Test
    @DisplayName("updateLog – fail: non-owner is forbidden")
    void updateLog_notOwner() {
        User otherFarmer = new User();
        otherFarmer.setId(2L);
        otherFarmer.setRole(Role.FARMER);

        when(farmService.getFarmOwnedBy(FARM_ID, otherFarmer))
                .thenThrow(new ForbiddenException("Not your farm"));

        assertThrows(ForbiddenException.class,
                () -> seasonLogService.updateLog(FARM_ID, LOG_ID, otherFarmer, logRequest));
    }

    // ─── deleteLog ──────────────────────────────────────────────────────

    /** Happy path: an owner can delete their own log. */
    @Test
    @DisplayName("deleteLog – success: owner deletes log")
    void deleteLog_success() {
        when(farmService.getFarmOwnedBy(FARM_ID, farmer)).thenReturn(farm);
        when(seasonLogRepository.findById(LOG_ID)).thenReturn(Optional.of(sampleLog));

        seasonLogService.deleteLog(FARM_ID, LOG_ID, farmer);

        verify(seasonLogRepository).delete(sampleLog);
    }

    /** Fail: deleting an unknown log id. */
    @Test
    @DisplayName("deleteLog – fail: log not found")
    void deleteLog_notFound() {
        when(seasonLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> seasonLogService.deleteLog(FARM_ID, 999L, farmer));
    }

    /** Fail: a farmer cannot delete a log on someone else's farm. */
    @Test
    @DisplayName("deleteLog – fail: non-owner is forbidden")
    void deleteLog_notOwner() {
        User otherFarmer = new User();
        otherFarmer.setId(2L);
        otherFarmer.setRole(Role.FARMER);

        when(farmService.getFarmOwnedBy(FARM_ID, otherFarmer))
                .thenThrow(new ForbiddenException("Not your farm"));

        assertThrows(ForbiddenException.class,
                () -> seasonLogService.deleteLog(FARM_ID, LOG_ID, otherFarmer));
    }
}
