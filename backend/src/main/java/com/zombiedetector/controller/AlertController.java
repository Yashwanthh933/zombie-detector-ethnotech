package com.zombiedetector.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.Alert;
import com.zombiedetector.repository.AlertRepository;
import com.zombiedetector.service.AlertService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertService alertService;

    public AlertController(AlertRepository alertRepository, AlertService alertService) {
        this.alertRepository = alertRepository;
        this.alertService = alertService;
    }

    @GetMapping
    public List<Alert> getAlerts() {
        return alertRepository.findAllByOrderByTimestampDesc();
    }

    @PostMapping("/{id}/ack")
    public Alert acknowledge(@PathVariable Long id) {
        return alertService.acknowledge(id);
    }
}