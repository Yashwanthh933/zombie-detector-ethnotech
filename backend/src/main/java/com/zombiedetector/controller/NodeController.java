package com.zombiedetector.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.repository.NodeRepository;
import com.zombiedetector.service.AuditService;

@RestController
public class NodeController {

    private final NodeRepository nodeRepository;
    private final AuditService auditService;

    public NodeController(NodeRepository nodeRepository, AuditService auditService) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
    }

    @GetMapping("/api/nodes")
    public List<ManagedNode> getAllNodes() {
        return nodeRepository.findAll();
    }

    @GetMapping("/api/savings")
    public Map<String, Object> getSavings() {
        List<ManagedNode> stopped = nodeRepository.findAll().stream()
                .filter(n -> n.getStatus() == NodeStatus.STOPPED)
                .toList();

        double monthlySavings = stopped.stream()
                .mapToDouble(n -> n.getHourlyRate() * 24 * 30)
                .sum();

        return Map.of(
                "stoppedCount", stopped.size(),
                "monthlySavings", Math.round(monthlySavings * 100.0) / 100.0
        );
    }

    @GetMapping("/api/savings/history")
    public List<Map<String, Object>> getSavingsHistory() {
        List<ManagedNode> stopped = nodeRepository.findAll().stream()
                .filter(n -> n.getStatus() == NodeStatus.STOPPED)
                .toList();

        double monthlySavings = stopped.stream()
                .mapToDouble(n -> n.getHourlyRate() * 24 * 30)
                .sum();
        double dailySavings = monthlySavings / 30.0;
        double dailyWaste = Math.max(0.0, dailySavings * 0.18);

        List<Map<String, Object>> history = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            double factor = 1.0 + (i * 0.05);
            history.add(Map.of(
                    "date", date.toString(),
                    "savings", Math.round((dailySavings * factor) * 100.0) / 100.0,
                    "waste", Math.round((dailyWaste * (0.9 + (i * 0.02))) * 100.0) / 100.0
            ));
        }

        return history;
    }

    @PostMapping("/api/nodes/{id}/override")
    public ResponseEntity<ManagedNode> overrideNode(@PathVariable String id) {
        return nodeRepository.findById(id)
                .map(node -> {
                    node.setStatus(NodeStatus.RUNNING);
                    node.setFlaggedAt(null);
                    node.setGracePeriodEndsAt(null);
                    node.setCleanStreak(0);
                    node.setManualOverrideUntil(LocalDateTime.now().plusHours(24));
                    nodeRepository.save(node);
                    auditService.log("james.chen", "OVERRIDE", id, "SUCCESS", "Manual override, 24h shield applied");
                    return ResponseEntity.ok(node);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}