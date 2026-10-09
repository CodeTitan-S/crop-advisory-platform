package com.college.cropadvisory.client;

import com.college.cropadvisory.model.entity.SoilReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class MlServiceClient {

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MlServiceClient(RestTemplate restTemplate,
                           @Value("${app.ml-service.url}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public Map<String, Object> getSuggestions(SoilReading reading) {
        Map<String, Double> payload = Map.of(
            "N", reading.getNitrogen(),
            "P", reading.getPhosphorus(),
            "K", reading.getPotassium(),
            "temperature", reading.getTemperature(),
            "humidity", reading.getHumidity(),
            "ph", reading.getPh(),
            "rainfall", reading.getRainfall()
        );
        return restTemplate.postForObject(mlServiceUrl + "/predict", payload, Map.class);
    }
}
