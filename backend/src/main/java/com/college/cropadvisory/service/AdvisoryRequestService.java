package com.college.cropadvisory.service;

import com.college.cropadvisory.model.entity.*;
import com.college.cropadvisory.repository.AdvisoryRequestRepository;
import com.college.cropadvisory.repository.FarmRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdvisoryRequestService {

    private final AdvisoryRequestRepository advisoryRequestRepository;
    private final FarmRepository farmRepository;

    public AdvisoryRequestService(AdvisoryRequestRepository advisoryRequestRepository,
                                  FarmRepository farmRepository) {
        this.advisoryRequestRepository = advisoryRequestRepository;
        this.farmRepository = farmRepository;
    }

    public AdvisoryRequest submitRequest(User farmer, Long farmId, String question) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));
        if (!farm.getUser().getId().equals(farmer.getId())) {
            throw new RuntimeException("Not your farm");
        }
        AdvisoryRequest request = new AdvisoryRequest();
        request.setFarmer(farmer);
        request.setFarm(farm);
        request.setQuestionText(question);
        request.setStatus(AdvisoryStatus.PENDING);
        return advisoryRequestRepository.save(request);
    }

    public List<AdvisoryRequest> getRequestsForFarmer(User farmer) {
        return advisoryRequestRepository.findByFarmerOrderByCreatedAtDesc(farmer);
    }

    public List<AdvisoryRequest> getRequestsForOfficer(User officer) {
        // Returns all requests where officer is assigned, or no officer yet (pending)
        return advisoryRequestRepository.findByOfficerOrOfficerIsNullOrderByCreatedAtDesc(officer);
    }

    public AdvisoryRequest assignToOfficer(Long requestId, User officer) {
        AdvisoryRequest req = advisoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (req.getStatus() != AdvisoryStatus.PENDING) {
            throw new RuntimeException("Request is not in PENDING status");
        }
        req.setOfficer(officer);
        req.setStatus(AdvisoryStatus.ASSIGNED);
        return advisoryRequestRepository.save(req);
    }

    public AdvisoryRequest respondToRequest(Long requestId, User officer, String responseText) {
        AdvisoryRequest req = advisoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (req.getOfficer() == null || !req.getOfficer().getId().equals(officer.getId())) {
            throw new RuntimeException("Not assigned to you");
        }
        if (req.getStatus() != AdvisoryStatus.ASSIGNED) {
            throw new RuntimeException("Request must be in ASSIGNED status");
        }
        req.setResponseText(responseText);
        req.setStatus(AdvisoryStatus.RESPONDED);
        req.setRespondedAt(LocalDateTime.now());
        return advisoryRequestRepository.save(req);
    }

    public AdvisoryRequest closeRequest(Long requestId, User user) {
        AdvisoryRequest req = advisoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        // Only assigned officer or admin can close
        boolean isAssignedOfficer = req.getOfficer() != null && req.getOfficer().getId().equals(user.getId());
        boolean isAdmin = user.getRole().equals(Role.ADMIN);
        if (!isAssignedOfficer && !isAdmin) {
            throw new RuntimeException("Not authorized to close this request");
        }
        if (req.getStatus() != AdvisoryStatus.RESPONDED) {
            throw new RuntimeException("Request must be RESPONDED before closing");
        }
        req.setStatus(AdvisoryStatus.CLOSED);
        return advisoryRequestRepository.save(req);
    }
}