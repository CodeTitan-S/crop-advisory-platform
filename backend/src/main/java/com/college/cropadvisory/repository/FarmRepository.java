package com.college.cropadvisory.repository;

import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    List<Farm> findByUser(User user);
}