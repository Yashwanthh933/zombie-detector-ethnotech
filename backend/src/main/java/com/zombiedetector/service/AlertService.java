package com.zombiedetector.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.zombiedetector.model.Alert;
import com.zombiedetector.repository.AlertRepository;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /** Avoids spamming duplicate alerts for the same ongoing, unacknowledged condition. */
    public void raiseIfNotDuplicate(String severity, String title, String message, String targetNodeId) {
        if (alertRepository.existsByTargetNodeIdAndTitleAndAcknowledgedFalse(targetNodeId, title)) return;

        Alert a = new Alert();
        a.setTimestamp(LocalDateTime.now());
        a.setSeverity(severity);
        a.setTitle(title);
        a.setMessage(message);
        a.setTargetNodeId(targetNodeId);
        a.setAcknowledged(false);
        alertRepository.save(a);
    }

    public Alert acknowledge(Long id) {
        Alert a = alertRepository.findById(id).orElseThrow();
        a.setAcknowledged(true);
        return alertRepository.save(a);
    }
}