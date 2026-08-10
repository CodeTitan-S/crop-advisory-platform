// AdvisoryRequestRepository.java
package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.AdvisoryRequest;
import com.college.cropadvisory.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvisoryRequestRepository extends JpaRepository<AdvisoryRequest, Long> {
    List<AdvisoryRequest> findByFarmerOrderByCreatedAtDesc(User farmer);
    List<AdvisoryRequest> findByOfficerOrOfficerIsNullOrderByCreatedAtDesc(User officer);
}