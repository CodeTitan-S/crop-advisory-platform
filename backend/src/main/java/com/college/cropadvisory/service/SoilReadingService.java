package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.SoilReadingRequest;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SoilReading;
import com.college.cropadvisory.repository.SoilReadingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SoilReadingService {

    private final SoilReadingRepository soilReadingRepository;

    public SoilReadingService(SoilReadingRepository soilReadingRepository) {
        this.soilReadingRepository = soilReadingRepository;
    }

    public SoilReading logReading(Farm farm, SoilReadingRequest request) {
        SoilReading reading = new SoilReading();
        reading.setFarm(farm);
        reading.setNitrogen(request.getNitrogen());
        reading.setPhosphorus(request.getPhosphorus());
        reading.setPotassium(request.getPotassium());
        reading.setPh(request.getPh());
        reading.setRainfall(request.getRainfall());
        reading.setTemperature(request.getTemperature());
        reading.setRecordedAt(LocalDateTime.now());
        return soilReadingRepository.save(reading);
    }

    public List<SoilReading> getReadingsByFarm(Farm farm) {
        return soilReadingRepository.findByFarmOrderByRecordedAtDesc(farm);
    }

    public SoilReading getLatestReading(Farm farm) {
        return soilReadingRepository.findFirstByFarmOrderByRecordedAtDesc(farm)
                .orElse(null);
    }
}