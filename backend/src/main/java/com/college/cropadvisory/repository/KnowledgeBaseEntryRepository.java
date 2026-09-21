package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeBaseEntryRepository extends JpaRepository<KnowledgeBaseEntry, Long> {

    List<KnowledgeBaseEntry> findAllByOrderByCropOrDiseaseNameAsc();

    /**
     * Case-insensitive duplicate-name check.
     *
     * <p>Written as an explicit query on purpose: the entity property is
     * {@code cropOrDiseaseName}, and Spring Data parses a bare "Or" inside a derived-query name as
     * the OR keyword. The derived form {@code existsByCropOrDiseaseNameIgnoreCase} is therefore
     * read as {@code crop OR diseaseName} and blows up at startup with
     * "No property 'crop' found" — so a derived name simply cannot be used here.
     */
    @Query("SELECT COUNT(e) > 0 FROM KnowledgeBaseEntry e "
            + "WHERE LOWER(e.cropOrDiseaseName) = LOWER(:name)")
    boolean existsByCropOrDiseaseNameIgnoreCase(@Param("name") String cropOrDiseaseName);

    long countByCreatedByAdmin(User admin);
}
