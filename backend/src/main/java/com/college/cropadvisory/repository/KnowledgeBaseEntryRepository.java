package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseEntryRepository extends JpaRepository<KnowledgeBaseEntry, Long> {
}