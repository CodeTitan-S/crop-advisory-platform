package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SeasonLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonLogRepository extends JpaRepository<SeasonLog, Long> {

    List<SeasonLog> findByFarmOrderBySowingDateDesc(Farm farm);
}
