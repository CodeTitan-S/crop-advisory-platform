package com.college.cropadvisory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** Platform-wide counts backing the admin analytics view. */
@Data
@AllArgsConstructor
public class AnalyticsResponse {

    private long totalUsers;
    private Map<String, Long> usersByRole;

    private long totalFarms;

    private long totalAdvisoryRequests;
    private Map<String, Long> requestsByStatus;

    private long totalDiseaseReports;
    private Map<String, Long> reportsByStatus;

    /** Advisory requests created in each of the last eight ISO weeks, oldest first. */
    private List<WeeklyCount> requestsPerWeek;

    /**
     * Knowledge-base entry names ranked by how many disease reports mention them. Report
     * descriptions are free text, so this is a keyword match rather than a classification.
     */
    private List<KeywordCount> topDiseases;

    @Data
    @AllArgsConstructor
    public static class WeeklyCount {
        /** ISO date of that week's Monday. */
        private String weekStarting;
        private long count;
    }

    @Data
    @AllArgsConstructor
    public static class KeywordCount {
        private String name;
        private long count;
    }
}
