package com.zombiedetector.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.User;
import com.zombiedetector.service.AuditService;
import com.zombiedetector.service.SchedulerState;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerState schedulerState;
    private final AuditService auditService;

    public SchedulerController(SchedulerState schedulerState, AuditService auditService) {
        this.schedulerState = schedulerState;
        this.auditService = auditService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("paused", schedulerState.isPaused());
    }

    /** Global kill switch -- admin-only, this affects every user's resources at once. */
    @PostMapping("/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> pause(@AuthenticationPrincipal User admin) {
        schedulerState.pause();
        auditService.log(admin.getEmail(), "SCHEDULER_PAUSED", null, "SUCCESS", "Automation paused for all users");
        return Map.of("paused", true);
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> resume(@AuthenticationPrincipal User admin) {
        schedulerState.resume();
        auditService.log(admin.getEmail(), "SCHEDULER_RESUMED", null, "SUCCESS", "Automation resumed for all users");
        return Map.of("paused", false);
    }
}
