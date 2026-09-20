package com.college.cropadvisory.service;

import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.*;
import com.college.cropadvisory.repository.AdvisoryRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdvisoryRequestService {

    private final AdvisoryRequestRepository advisoryRequestRepository;
    private final FarmService farmService;

    public AdvisoryRequestService(AdvisoryRequestRepository advisoryRequestRepository,
                                  FarmService farmService) {
        this.advisoryRequestRepository = advisoryRequestRepository;
        this.farmService = farmService;
    }

    public AdvisoryRequest submitRequest(User farmer, Long farmId, String question) {
        Farm farm = farmService.getFarmOwnedBy(farmId, farmer);
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
        AdvisoryRequest req = getRequest(requestId);
        if (req.getStatus() != AdvisoryStatus.PENDING) {
            throw new ConflictException("Request is not in PENDING status");
        }
        req.setOfficer(officer);
        req.setStatus(AdvisoryStatus.ASSIGNED);
        return advisoryRequestRepository.save(req);
    }

    public AdvisoryRequest respondToRequest(Long requestId, User officer, String responseText) {
        AdvisoryRequest req = getRequest(requestId);
        requireAssignedTo(req, officer);
        if (req.getStatus() != AdvisoryStatus.ASSIGNED) {
            throw new ConflictException("Request must be in ASSIGNED status");
        }
        req.setResponseText(responseText);
        req.setStatus(AdvisoryStatus.RESPONDED);
        req.setRespondedAt(LocalDateTime.now());
        return advisoryRequestRepository.save(req);
    }

    public AdvisoryRequest closeRequest(Long requestId, User user) {
        AdvisoryRequest req = getRequest(requestId);
        // Only the assigned officer or an admin can close
        boolean isAssignedOfficer = req.getOfficer() != null && req.getOfficer().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isAssignedOfficer && !isAdmin) {
            throw new ForbiddenException("Not authorized to close this request");
        }
        if (req.getStatus() != AdvisoryStatus.RESPONDED) {
            throw new ConflictException("Request must be RESPONDED before closing");
        }
        req.setStatus(AdvisoryStatus.CLOSED);
        return advisoryRequestRepository.save(req);
    }

    private AdvisoryRequest getRequest(Long requestId) {
        return advisoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
    }

    private void requireAssignedTo(AdvisoryRequest req, User officer) {
        if (req.getOfficer() == null || !req.getOfficer().getId().equals(officer.getId())) {
            throw new ForbiddenException("Not assigned to you");
        }
    }
}
