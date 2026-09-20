package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.KnowledgeBaseEntryRequest;
import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.KnowledgeBaseEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KnowledgeBaseService}.
 * Tests CRUD plus the duplicate-name protection that keeps entries addressable by name.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseEntryRepository knowledgeBaseEntryRepository;

    @InjectMocks
    private KnowledgeBaseService knowledgeBaseService;

    private User admin;
    private KnowledgeBaseEntryRequest request;
    private KnowledgeBaseEntry entry;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        request = new KnowledgeBaseEntryRequest();
        request.setCropOrDiseaseName("Leaf Blight");
        request.setDescription("Fungal disease causing water-soaked lesions on leaves");
        request.setRemedyOrAdvice("Remove affected leaves and apply a copper-based fungicide");
        request.setSource("ICAR advisory note");

        entry = new KnowledgeBaseEntry();
        entry.setId(50L);
        entry.setCreatedByAdmin(admin);
        entry.setCropOrDiseaseName("Leaf Blight");
        entry.setDescription("Fungal disease causing water-soaked lesions on leaves");
        entry.setRemedyOrAdvice("Remove affected leaves and apply a copper-based fungicide");
        entry.setSource("ICAR advisory note");
    }

    // ─── read ───────────────────────────────────────────────────────────

    /** Happy path: entries come back alphabetically. */
    @Test
    @DisplayName("getAll – success: returns entries ordered by name")
    void getAll_success() {
        KnowledgeBaseEntry second = new KnowledgeBaseEntry();
        second.setId(51L);
        second.setCropOrDiseaseName("Rust");
        when(knowledgeBaseEntryRepository.findAllByOrderByCropOrDiseaseNameAsc())
                .thenReturn(Arrays.asList(entry, second));

        List<KnowledgeBaseEntry> result = knowledgeBaseService.getAll();

        assertEquals(2, result.size());
        assertEquals("Leaf Blight", result.get(0).getCropOrDiseaseName());
        verify(knowledgeBaseEntryRepository).findAllByOrderByCropOrDiseaseNameAsc();
    }

    /** Happy path: an existing entry is returned. */
    @Test
    @DisplayName("getEntry – success: returns existing entry")
    void getEntry_success() {
        when(knowledgeBaseEntryRepository.findById(50L)).thenReturn(Optional.of(entry));

        assertEquals(entry, knowledgeBaseService.getEntry(50L));
    }

    /** Fail: unknown entry id. */
    @Test
    @DisplayName("getEntry – fail: entry not found")
    void getEntry_notFound() {
        when(knowledgeBaseEntryRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> knowledgeBaseService.getEntry(999L));

        assertEquals("Knowledge base entry not found", ex.getMessage());
    }

    // ─── create ─────────────────────────────────────────────────────────

    /** Happy path: the entry is saved and attributed to the creating admin. */
    @Test
    @DisplayName("create – success: saves entry attributed to the admin")
    void create_success() {
        when(knowledgeBaseEntryRepository.existsByCropOrDiseaseNameIgnoreCase("Leaf Blight"))
                .thenReturn(false);
        when(knowledgeBaseEntryRepository.save(any(KnowledgeBaseEntry.class))).thenReturn(entry);

        KnowledgeBaseEntry result = knowledgeBaseService.create(admin, request);

        assertEquals("Leaf Blight", result.getCropOrDiseaseName());
        assertEquals(admin, result.getCreatedByAdmin());
        assertEquals("ICAR advisory note", result.getSource());
        verify(knowledgeBaseEntryRepository).save(any(KnowledgeBaseEntry.class));
    }

    /** Fail: a name that is already in use, ignoring case. */
    @Test
    @DisplayName("create – fail: duplicate name is rejected")
    void create_duplicate() {
        when(knowledgeBaseEntryRepository.existsByCropOrDiseaseNameIgnoreCase("Leaf Blight"))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> knowledgeBaseService.create(admin, request));

        assertEquals("An entry with that name already exists", ex.getMessage());
    }

    // ─── update ─────────────────────────────────────────────────────────

    /** Happy path: fields are updated in place. */
    @Test
    @DisplayName("update – success: updates entry fields")
    void update_success() {
        KnowledgeBaseEntryRequest update = new KnowledgeBaseEntryRequest();
        update.setCropOrDiseaseName("Leaf Blight");
        update.setDescription("Updated description");
        update.setRemedyOrAdvice("Updated advice");
        update.setSource("Updated source");

        when(knowledgeBaseEntryRepository.findById(50L)).thenReturn(Optional.of(entry));
        when(knowledgeBaseEntryRepository.save(entry)).thenReturn(entry);

        KnowledgeBaseEntry result = knowledgeBaseService.update(50L, update);

        assertEquals("Updated description", result.getDescription());
        assertEquals("Updated advice", result.getRemedyOrAdvice());
        assertEquals("Updated source", result.getSource());
    }

    /** Happy path: keeping the same name is not treated as a duplicate. */
    @Test
    @DisplayName("update – success: unchanged name does not hit the duplicate check")
    void update_keepsOwnName() {
        KnowledgeBaseEntryRequest update = new KnowledgeBaseEntryRequest();
        update.setCropOrDiseaseName("leaf blight"); // same name, different case
        update.setDescription("Reworded");

        when(knowledgeBaseEntryRepository.findById(50L)).thenReturn(Optional.of(entry));
        when(knowledgeBaseEntryRepository.save(entry)).thenReturn(entry);

        KnowledgeBaseEntry result = knowledgeBaseService.update(50L, update);

        assertEquals("Reworded", result.getDescription());
    }

    /** Fail: renaming onto another entry's name. */
    @Test
    @DisplayName("update – fail: renaming onto an existing name is rejected")
    void update_renameToDuplicate() {
        KnowledgeBaseEntryRequest update = new KnowledgeBaseEntryRequest();
        update.setCropOrDiseaseName("Rust");

        when(knowledgeBaseEntryRepository.findById(50L)).thenReturn(Optional.of(entry));
        when(knowledgeBaseEntryRepository.existsByCropOrDiseaseNameIgnoreCase("Rust"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> knowledgeBaseService.update(50L, update));
    }

    // ─── delete ─────────────────────────────────────────────────────────

    /** Happy path: an existing entry is deleted. */
    @Test
    @DisplayName("delete – success: deletes entry")
    void delete_success() {
        when(knowledgeBaseEntryRepository.findById(50L)).thenReturn(Optional.of(entry));

        knowledgeBaseService.delete(50L);

        verify(knowledgeBaseEntryRepository).delete(entry);
    }

    /** Fail: deleting an unknown entry id. */
    @Test
    @DisplayName("delete – fail: entry not found")
    void delete_notFound() {
        when(knowledgeBaseEntryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> knowledgeBaseService.delete(999L));
    }
}
