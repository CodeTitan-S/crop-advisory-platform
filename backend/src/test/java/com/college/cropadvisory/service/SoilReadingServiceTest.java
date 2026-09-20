package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.SoilReadingRequest;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SoilReading;
import com.college.cropadvisory.repository.SoilReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SoilReadingService}.
 * Tests logging and listing of soil readings.
 */
@ExtendWith(MockitoExtension.class)
class SoilReadingServiceTest {

    @Mock
    private SoilReadingRepository soilReadingRepository;

    @InjectMocks
    private SoilReadingService soilReadingService;

    private Farm farm;
    private SoilReadingRequest readingRequest;
    private SoilReading sampleReading;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(10L);
        farm.setLocation("North Valley");

        readingRequest = new SoilReadingRequest();
        readingRequest.setNitrogen(40.0);
        readingRequest.setPhosphorus(20.0);
        readingRequest.setPotassium(30.0);
        readingRequest.setPh(6.5);
        readingRequest.setRainfall(120.0);
        readingRequest.setTemperature(28.0);

        sampleReading = new SoilReading();
        sampleReading.setId(100L);
        sampleReading.setFarm(farm);
        sampleReading.setNitrogen(40.0);
        sampleReading.setPhosphorus(20.0);
        sampleReading.setPotassium(30.0);
        sampleReading.setPh(6.5);
        sampleReading.setRainfall(120.0);
        sampleReading.setTemperature(28.0);
        sampleReading.setRecordedAt(LocalDateTime.now());
    }

    // ─── logReading ─────────────────────────────────────────────────────

    /** Happy path: reading is saved with correct nutrient values. */
    @Test
    @DisplayName("logReading – success: saves reading with correct values")
    void logReading_success() {
        when(soilReadingRepository.save(any(SoilReading.class))).thenReturn(sampleReading);

        SoilReading result = soilReadingService.logReading(farm, readingRequest);

        assertNotNull(result);
        assertEquals(40.0, result.getNitrogen());
        assertEquals(20.0, result.getPhosphorus());
        assertEquals(30.0, result.getPotassium());
        assertEquals(6.5, result.getPh());
        assertEquals(120.0, result.getRainfall());
        assertEquals(28.0, result.getTemperature());
        assertEquals(farm, result.getFarm());
        verify(soilReadingRepository).save(any(SoilReading.class));
    }

    // ─── getReadingsByFarm ──────────────────────────────────────────────

    /** Happy path: returns readings ordered by date (descending). */
    @Test
    @DisplayName("getReadingsByFarm – success: returns readings for farm")
    void getReadingsByFarm_success() {
        SoilReading older = new SoilReading();
        older.setId(101L);
        older.setFarm(farm);

        when(soilReadingRepository.findByFarmOrderByRecordedAtDesc(farm))
                .thenReturn(Arrays.asList(sampleReading, older));

        List<SoilReading> result = soilReadingService.getReadingsByFarm(farm);

        assertEquals(2, result.size());
        assertEquals(sampleReading, result.get(0));
        verify(soilReadingRepository).findByFarmOrderByRecordedAtDesc(farm);
    }

    /** Edge case: farm with no readings returns an empty list. */
    @Test
    @DisplayName("getReadingsByFarm – edge: no readings returns empty list")
    void getReadingsByFarm_empty() {
        when(soilReadingRepository.findByFarmOrderByRecordedAtDesc(farm))
                .thenReturn(Collections.emptyList());

        List<SoilReading> result = soilReadingService.getReadingsByFarm(farm);

        assertTrue(result.isEmpty());
    }
}
