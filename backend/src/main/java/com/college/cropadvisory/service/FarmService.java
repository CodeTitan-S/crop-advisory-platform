package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.FarmRequest;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.FarmRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FarmService {

    private final FarmRepository farmRepository;

    public FarmService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    public Farm createFarm(User farmer, FarmRequest request) {
        Farm farm = new Farm();
        farm.setLocation(request.getLocation());
        farm.setSize(request.getSize());
        farm.setSoilType(request.getSoilType());
        farm.setUser(farmer);
        return farmRepository.save(farm);
    }

    public List<Farm> getFarmsByUser(User user) {
        return farmRepository.findByUser(user);
    }

    /** Loads a farm or fails with 404. Does not check ownership. */
    public Farm getFarm(Long farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new NotFoundException("Farm not found"));
    }

    /**
     * Loads a farm the given user owns, failing with 404 if it does not exist and 403 if it
     * belongs to someone else. Single source of truth for farm ownership authorization.
     */
    public Farm getFarmOwnedBy(Long farmId, User user) {
        Farm farm = getFarm(farmId);
        if (!farm.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Not your farm");
        }
        return farm;
    }
}
