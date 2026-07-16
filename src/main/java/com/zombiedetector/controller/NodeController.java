package com.zombiedetector.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.repository.NodeRepository;

@RestController
public class NodeController {

    private final NodeRepository nodeRepository;

    public NodeController(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
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
}