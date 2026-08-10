package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.SeasonLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonLogRepository extends JpaRepository<SeasonLog, Long> {
}