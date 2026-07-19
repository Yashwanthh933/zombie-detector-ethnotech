package com.zombiedetector.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zombiedetector.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByOrderByTimestampDesc();
    boolean existsByTargetNodeIdAndTitleAndAcknowledgedFalse(String targetNodeId, String title);
}