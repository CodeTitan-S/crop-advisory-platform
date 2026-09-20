package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.DiseaseReportRequest;
import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.*;
import com.college.cropadvisory.repository.DiseaseReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiseaseReportService {

    private final DiseaseReportRepository diseaseReportRepository;
    private final FarmService farmService;

    public DiseaseReportService(DiseaseReportRepository diseaseReportRepository,
                                FarmService farmService) {
        this.diseaseReportRepository = diseaseReportRepository;
        this.farmService = farmService;
    }

    public DiseaseReport submitReport(User farmer, DiseaseReportRequest request) {
        Farm farm = farmService.getFarmOwnedBy(request.getFarmId(), farmer);
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
        DiseaseReport report = getReport(reportId);
        if (report.getStatus() != DiseaseStatus.REPORTED) {
            throw new ConflictException("Report is not REPORTED");
        }
        report.setOfficer(officer);
        report.setStatus(DiseaseStatus.UNDER_REVIEW);
        return diseaseReportRepository.save(report);
    }

    public DiseaseReport resolveReport(Long reportId, User officer, String resolutionNotes) {
        DiseaseReport report = getReport(reportId);
        if (report.getOfficer() == null || !report.getOfficer().getId().equals(officer.getId())) {
            throw new ForbiddenException("Not assigned to you");
        }
        if (report.getStatus() != DiseaseStatus.UNDER_REVIEW) {
            throw new ConflictException("Report must be UNDER_REVIEW");
        }
        report.setStatus(DiseaseStatus.RESOLVED);
        report.setResolutionNotes(resolutionNotes);
        report.setResolvedAt(LocalDateTime.now());
        return diseaseReportRepository.save(report);
    }

    private DiseaseReport getReport(Long reportId) {
        return diseaseReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));
    }
}
