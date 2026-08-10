package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.FarmRequest;
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
        // add a custom method in FarmRepository: findByUser
        return farmRepository.findByUser(user);
    }
}