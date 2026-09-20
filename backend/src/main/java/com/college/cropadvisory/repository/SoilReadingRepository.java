package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SoilReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SoilReadingRepository extends JpaRepository<SoilReading, Long> {
    List<SoilReading> findByFarmOrderByRecordedAtDesc(Farm farm);
}
