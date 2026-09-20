package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.KnowledgeBaseEntryRequest;
import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.KnowledgeBaseEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseEntryRepository knowledgeBaseEntryRepository;

    public KnowledgeBaseService(KnowledgeBaseEntryRepository knowledgeBaseEntryRepository) {
        this.knowledgeBaseEntryRepository = knowledgeBaseEntryRepository;
    }

    public List<KnowledgeBaseEntry> getAll() {
        return knowledgeBaseEntryRepository.findAllByOrderByCropOrDiseaseNameAsc();
    }

    public KnowledgeBaseEntry getEntry(Long id) {
        return knowledgeBaseEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Knowledge base entry not found"));
    }

    public KnowledgeBaseEntry create(User admin, KnowledgeBaseEntryRequest request) {
        if (knowledgeBaseEntryRepository
                .existsByCropOrDiseaseNameIgnoreCase(request.getCropOrDiseaseName())) {
            throw new ConflictException("An entry with that name already exists");
        }
        KnowledgeBaseEntry entry = new KnowledgeBaseEntry();
        entry.setCreatedByAdmin(admin);
        applyRequest(entry, request);
        return knowledgeBaseEntryRepository.save(entry);
    }

    public KnowledgeBaseEntry update(Long id, KnowledgeBaseEntryRequest request) {
        KnowledgeBaseEntry entry = getEntry(id);
        // Renaming an entry onto another entry's name would create a duplicate.
        boolean renaming = !entry.getCropOrDiseaseName()
                .equalsIgnoreCase(request.getCropOrDiseaseName());
        if (renaming && knowledgeBaseEntryRepository
                .existsByCropOrDiseaseNameIgnoreCase(request.getCropOrDiseaseName())) {
            throw new ConflictException("An entry with that name already exists");
        }
        applyRequest(entry, request);
        return knowledgeBaseEntryRepository.save(entry);
    }

    public void delete(Long id) {
        knowledgeBaseEntryRepository.delete(getEntry(id));
    }

    private void applyRequest(KnowledgeBaseEntry entry, KnowledgeBaseEntryRequest request) {
        entry.setCropOrDiseaseName(request.getCropOrDiseaseName());
        entry.setDescription(request.getDescription());
        entry.setRemedyOrAdvice(request.getRemedyOrAdvice());
        entry.setSource(request.getSource());
    }
}
