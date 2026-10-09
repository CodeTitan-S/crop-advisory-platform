package com.college.cropadvisory.client;

import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SoilReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private MlServiceClient mlServiceClient;
    private final String mlUrl = "http://localhost:8000";

    @BeforeEach
    void setUp() {
        mlServiceClient = new MlServiceClient(restTemplate, mlUrl);
    }

    @Test
    @DisplayName("getSuggestions – success: returns suggestion map from ML service")
    void getSuggestions_success() {
        SoilReading reading = new SoilReading();
        reading.setNitrogen(10.0);
        reading.setPhosphorus(20.0);
        reading.setPotassium(30.0);
        reading.setPh(6.5);
        reading.setRainfall(100.0);
        reading.setTemperature(25.0);
        reading.setHumidity(60.0);

        Map<String, Object> expected = Map.of("recommendations", "rice,maize,chickpea");
        when(restTemplate.postForObject(eq(mlUrl + "/predict"), any(), eq(Map.class)))
                .thenReturn(expected);

        Map<String, Object> result = mlServiceClient.getSuggestions(reading);

        assertEquals(expected, result);
    }
}
