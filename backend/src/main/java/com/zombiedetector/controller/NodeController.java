package com.zombiedetector.controller;

import java.time.LocalDateTime;
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