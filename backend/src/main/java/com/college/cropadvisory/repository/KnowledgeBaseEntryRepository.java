package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeBaseEntryRepository extends JpaRepository<KnowledgeBaseEntry, Long> {

    List<KnowledgeBaseEntry> findAllByOrderByCropOrDiseaseNameAsc();

    boolean existsByCropOrDiseaseNameIgnoreCase(String cropOrDiseaseName);

    long countByCreatedByAdmin(User admin);
}
