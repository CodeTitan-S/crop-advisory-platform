package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.DiseaseReportRequest;
import com.college.cropadvisory.model.entity.*;
import com.college.cropadvisory.repository.DiseaseReportRepository;
import com.college.cropadvisory.repository.FarmRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiseaseReportService {

    private final DiseaseReportRepository diseaseReportRepository;
    private final FarmRepository farmRepository;

    public DiseaseReportService(DiseaseReportRepository diseaseReportRepository,
                                FarmRepository farmRepository) {
        this.diseaseReportRepository = diseaseReportRepository;
        this.farmRepository = farmRepository;
    }

    public DiseaseReport submitReport(User farmer, DiseaseReportRequest request) {
        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new RuntimeException("Farm not found"));
        if (!farm.getUser().getId().equals(farmer.getId())) {
            throw new RuntimeException("Not your farm");
        }
        DiseaseReport report = new DiseaseReport();
        report.setFarmer(farmer);
        report.setFarm(farm);
        report.setDescription(request.getDescription());
        report.setImageUrl(request.getImageUrl());
        report.setStatus(DiseaseStatus.REPORTED);
        return diseaseReportRepository.save(report);
    }

    public List<DiseaseReport> getReportsForFarmer(User farmer) {
        return diseaseReportRepository.findByFarmerOrderByCreatedAtDesc(farmer);
    }

    public List<DiseaseReport> getReportsForOfficer(User officer) {
        return diseaseReportRepository.findByOfficerOrOfficerIsNullOrderByCreatedAtDesc(officer);
    }

    public DiseaseReport reviewReport(Long reportId, User officer) {
        DiseaseReport report = diseaseReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        if (report.getStatus() != DiseaseStatus.REPORTED) {
            throw new RuntimeException("Report is not REPORTED");
        }
        report.setOfficer(officer);
        report.setStatus(DiseaseStatus.UNDER_REVIEW);
        return diseaseReportRepository.save(report);
    }

    public DiseaseReport resolveReport(Long reportId, User officer, String resolutionNotes) {
        DiseaseReport report = diseaseReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        if (!report.getOfficer().getId().equals(officer.getId())) {
            throw new RuntimeException("Not assigned to you");
        }
        if (report.getStatus() != DiseaseStatus.UNDER_REVIEW) {
            throw new RuntimeException("Report must be UNDER_REVIEW");
        }
        report.setStatus(DiseaseStatus.RESOLVED);
        report.setResolutionNotes(resolutionNotes);
        report.setResolvedAt(LocalDateTime.now());
        return diseaseReportRepository.save(report);
    }
}