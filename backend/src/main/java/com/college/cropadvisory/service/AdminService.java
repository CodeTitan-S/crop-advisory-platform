package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.AnalyticsResponse;
import com.college.cropadvisory.dto.UserSummaryResponse;
import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.AdvisoryRequest;
import com.college.cropadvisory.model.entity.DiseaseReport;
import com.college.cropadvisory.model.entity.KnowledgeBaseEntry;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.AdvisoryRequestRepository;
import com.college.cropadvisory.repository.DiseaseReportRepository;
import com.college.cropadvisory.repository.FarmRepository;
import com.college.cropadvisory.repository.KnowledgeBaseEntryRepository;
import com.college.cropadvisory.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

@Service
public class AdminService {

    private static final int ANALYTICS_WEEKS = 8;
    private static final int TOP_DISEASE_LIMIT = 5;

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final AdvisoryRequestRepository advisoryRequestRepository;
    private final DiseaseReportRepository diseaseReportRepository;
    private final KnowledgeBaseEntryRepository knowledgeBaseEntryRepository;

    public AdminService(UserRepository userRepository,
                        FarmRepository farmRepository,
                        AdvisoryRequestRepository advisoryRequestRepository,
                        DiseaseReportRepository diseaseReportRepository,
                        KnowledgeBaseEntryRepository knowledgeBaseEntryRepository) {
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.advisoryRequestRepository = advisoryRequestRepository;
        this.diseaseReportRepository = diseaseReportRepository;
        this.knowledgeBaseEntryRepository = knowledgeBaseEntryRepository;
    }

    // ─── User management ────────────────────────────────────────────────

    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(user -> UserSummaryResponse.of(user, farmRepository.countByUser(user)))
                .toList();
    }

    public UserSummaryResponse updateUserRole(User actingAdmin, Long userId, String role) {
        User target = getUser(userId);
        Role newRole = parseRole(role);

        if (newRole == target.getRole()) {
            return UserSummaryResponse.of(target, farmRepository.countByUser(target));
        }
        // Changing your own role could lock you out of the admin screens entirely.
        if (isSameUser(target, actingAdmin)) {
            throw new ConflictException("You cannot change your own role");
        }
        if (target.getRole() == Role.ADMIN && countAdmins() <= 1) {
            throw new ConflictException("Cannot change the role of the last remaining admin");
        }

        target.setRole(newRole);
        return UserSummaryResponse.of(userRepository.save(target), farmRepository.countByUser(target));
    }

    public void deleteUser(User actingAdmin, Long userId) {
        User target = getUser(userId);

        if (isSameUser(target, actingAdmin)) {
            throw new ConflictException("You cannot delete your own account");
        }
        if (target.getRole() == Role.ADMIN && countAdmins() <= 1) {
            throw new ConflictException("Cannot delete the last remaining admin");
        }

        // AdvisoryRequest.farmer/officer and DiseaseReport.farmer/officer are @ManyToOne without a
        // cascade, so deleting a user that still owns workflow records would violate a foreign key.
        long dependents = farmRepository.countByUser(target)
                + advisoryRequestRepository.countByFarmer(target)
                + advisoryRequestRepository.countByOfficer(target)
                + diseaseReportRepository.countByFarmer(target)
                + diseaseReportRepository.countByOfficer(target)
                + knowledgeBaseEntryRepository.countByCreatedByAdmin(target);
        if (dependents > 0) {
            throw new ConflictException(
                    "User still owns " + dependents + " farms or workflow records; remove those first");
        }

        userRepository.delete(target);
    }

    // ─── Analytics ──────────────────────────────────────────────────────

    public AnalyticsResponse getAnalytics() {
        List<User> users = userRepository.findAll();
        List<AdvisoryRequest> requests = advisoryRequestRepository.findAll();
        List<DiseaseReport> reports = diseaseReportRepository.findAll();

        return new AnalyticsResponse(
                users.size(),
                countBy(users, user -> user.getRole().name()),
                farmRepository.count(),
                requests.size(),
                countBy(requests, request -> request.getStatus().name()),
                reports.size(),
                countBy(reports, report -> report.getStatus().name()),
                weeklyCounts(requests.stream().map(AdvisoryRequest::getCreatedAt).toList()),
                topDiseases(reports));
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private long countAdmins() {
        return userRepository.countByRole(Role.ADMIN);
    }

    private boolean isSameUser(User target, User actingAdmin) {
        return target.getId() != null && target.getId().equals(actingAdmin.getId());
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown role: " + role);
        }
    }

    private static <T> Map<String, Long> countBy(List<T> items, Function<T, String> keyFn) {
        Map<String, Long> counts = new TreeMap<>();
        for (T item : items) {
            counts.merge(keyFn.apply(item), 1L, Long::sum);
        }
        return counts;
    }

    /** Advisory requests per ISO week over the last {@value #ANALYTICS_WEEKS} weeks, zeros filled. */
    private List<AnalyticsResponse.WeeklyCount> weeklyCounts(List<LocalDateTime> createdAtList) {
        LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int weeksAgo = ANALYTICS_WEEKS - 1; weeksAgo >= 0; weeksAgo--) {
            counts.put(currentMonday.minusWeeks(weeksAgo), 0L);
        }
        for (LocalDateTime createdAt : createdAtList) {
            if (createdAt == null) {
                continue;
            }
            LocalDate week = createdAt.toLocalDate().with(DayOfWeek.MONDAY);
            counts.computeIfPresent(week, (key, value) -> value + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new AnalyticsResponse.WeeklyCount(entry.getKey().toString(), entry.getValue()))
                .toList();
    }

    /**
     * Ranks knowledge-base entry names by how many disease-report descriptions mention them.
     * Descriptions are free text with no structured disease field, so this is a deliberate keyword
     * match rather than a classification — an empty knowledge base yields an empty list.
     */
    private List<AnalyticsResponse.KeywordCount> topDiseases(List<DiseaseReport> reports) {
        List<String> descriptions = reports.stream()
                .map(DiseaseReport::getDescription)
                .filter(Objects::nonNull)
                .map(description -> description.toLowerCase(Locale.ROOT))
                .toList();
        if (descriptions.isEmpty()) {
            return List.of();
        }

        return knowledgeBaseEntryRepository.findAll().stream()
                .map(KnowledgeBaseEntry::getCropOrDiseaseName)
                .filter(Objects::nonNull)
                .map(name -> {
                    String needle = name.toLowerCase(Locale.ROOT);
                    long matches = descriptions.stream().filter(d -> d.contains(needle)).count();
                    return new AnalyticsResponse.KeywordCount(name, matches);
                })
                .filter(count -> count.getCount() > 0)
                .sorted(Comparator.comparingLong(AnalyticsResponse.KeywordCount::getCount)
                        .reversed()
                        .thenComparing(AnalyticsResponse.KeywordCount::getName))
                .limit(TOP_DISEASE_LIMIT)
                .toList();
    }
}
