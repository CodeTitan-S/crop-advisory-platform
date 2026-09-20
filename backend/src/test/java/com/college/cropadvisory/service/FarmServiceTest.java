package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.FarmRequest;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.FarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FarmService}.
 * Tests farm creation, retrieval by user, and ownership authorization.
 */
@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @InjectMocks
    private FarmService farmService;

    private User farmer;
    private User otherFarmer;
    private FarmRequest farmRequest;
    private Farm sampleFarm;

    @BeforeEach
    void setUp() {
        farmer = new User();
        farmer.setId(1L);
        farmer.setName("Jane Farmer");
        farmer.setEmail("jane@example.com");
        farmer.setRole(Role.FARMER);

        otherFarmer = new User();
        otherFarmer.setId(2L);
        otherFarmer.setName("Eve Farmer");
        otherFarmer.setEmail("eve@example.com");
        otherFarmer.setRole(Role.FARMER);

        farmRequest = new FarmRequest();
        farmRequest.setLocation("North Valley");
        farmRequest.setSize(25.5);
        farmRequest.setSoilType("Loamy");

        sampleFarm = new Farm();
        sampleFarm.setId(10L);
        sampleFarm.setLocation("North Valley");
        sampleFarm.setSize(25.5);
        sampleFarm.setSoilType("Loamy");
        sampleFarm.setUser(farmer);
    }

    // ─── createFarm ─────────────────────────────────────────────────────

    /** Happy path: farm is created with correct fields and linked to the user. */
    @Test
    @DisplayName("createFarm – success: farm is saved with correct fields")
    void createFarm_success() {
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        Farm result = farmService.createFarm(farmer, farmRequest);

        assertNotNull(result);
        assertEquals("North Valley", result.getLocation());
        assertEquals(25.5, result.getSize());
        assertEquals("Loamy", result.getSoilType());
        assertEquals(farmer, result.getUser());
        verify(farmRepository).save(any(Farm.class));
    }

    // ─── getFarmsByUser ─────────────────────────────────────────────────

    /** Happy path: returns all farms belonging to the user. */
    @Test
    @DisplayName("getFarmsByUser – success: returns farms for user")
    void getFarmsByUser_success() {
        Farm farm2 = new Farm();
        farm2.setId(11L);
        farm2.setLocation("South Fields");
        farm2.setSize(10.0);
        farm2.setSoilType("Sandy");
        farm2.setUser(farmer);

        when(farmRepository.findByUser(farmer)).thenReturn(Arrays.asList(sampleFarm, farm2));

        List<Farm> result = farmService.getFarmsByUser(farmer);

        assertEquals(2, result.size());
        verify(farmRepository).findByUser(farmer);
    }

    /** Edge case: user with no farms returns an empty list. */
    @Test
    @DisplayName("getFarmsByUser – edge: user with no farms returns empty list")
    void getFarmsByUser_noFarms() {
        when(farmRepository.findByUser(farmer)).thenReturn(Collections.emptyList());

        List<Farm> result = farmService.getFarmsByUser(farmer);

        assertTrue(result.isEmpty());
    }

    // ─── getFarm ────────────────────────────────────────────────────────

    /** Happy path: existing farm is returned. */
    @Test
    @DisplayName("getFarm – success: returns existing farm")
    void getFarm_success() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        Farm result = farmService.getFarm(10L);

        assertEquals(sampleFarm, result);
    }

    /** Fail: unknown farm id. */
    @Test
    @DisplayName("getFarm – fail: farm not found")
    void getFarm_notFound() {
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> farmService.getFarm(999L));

        assertEquals("Farm not found", ex.getMessage());
    }

    // ─── getFarmOwnedBy ─────────────────────────────────────────────────

    /** Happy path: owner can access their farm. */
    @Test
    @DisplayName("getFarmOwnedBy – success: owner can access farm")
    void getFarmOwnedBy_success() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        Farm result = farmService.getFarmOwnedBy(10L, farmer);

        assertEquals(sampleFarm, result);
    }

    /** Fail: a different user cannot access the farm. */
    @Test
    @DisplayName("getFarmOwnedBy – fail: non-owner is forbidden")
    void getFarmOwnedBy_notOwner() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> farmService.getFarmOwnedBy(10L, otherFarmer));

        assertEquals("Not your farm", ex.getMessage());
    }

    /** Fail: unknown farm id. */
    @Test
    @DisplayName("getFarmOwnedBy – fail: farm not found")
    void getFarmOwnedBy_notFound() {
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> farmService.getFarmOwnedBy(999L, farmer));

        assertEquals("Farm not found", ex.getMessage());
    }
}
