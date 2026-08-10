// DiseaseReportRepository.java
package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.DiseaseReport;
import com.college.cropadvisory.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiseaseReportRepository extends JpaRepository<DiseaseReport, Long> {
    List<DiseaseReport> findByFarmerOrderByCreatedAtDesc(User farmer);
    List<DiseaseReport> findByOfficerOrOfficerIsNullOrderByCreatedAtDesc(User officer);
}