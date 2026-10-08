package com.zombiedetector.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.Alert;
import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.Role;
import com.zombiedetector.model.User;
import com.zombiedetector.repository.AlertRepository;
import com.zombiedetector.repository.NodeRepository;
import com.zombiedetector.service.AlertService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final NodeRepository nodeRepository;

    public AlertController(AlertRepository alertRepository, AlertService alertService, NodeRepository nodeRepository) {
        this.alertRepository = alertRepository;
        this.alertService = alertService;
        this.nodeRepository = nodeRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAlerts(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        List<Alert> all = alertRepository.findAllByOrderByTimestampDesc();

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(all);
        }

        Set<String> ownedNodeIds = nodeRepository.findByOwnerId(user.getId()).stream()
                .map(ManagedNode::getNodeId)
                .collect(Collectors.toSet());

        List<Alert> scoped = all.stream()
                .filter(a -> ownedNodeIds.contains(a.getTargetNodeId()))
                .toList();

        return ResponseEntity.ok(scoped);
    }

    /** Now owner-or-admin gated: only the owner of the alert's target node (or an admin) can dismiss it. */
    @PostMapping("/{id}/ack")
    public ResponseEntity<?> acknowledge(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }

        var maybeAlert = alertRepository.findById(id);
        if (maybeAlert.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Alert alert = maybeAlert.get();

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = false;
        if (!isAdmin) {
            var maybeNode = nodeRepository.findById(alert.getTargetNodeId());
            isOwner = maybeNode.isPresent() && user.getId().equals(maybeNode.get().getOwnerId());
        }

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).body("You don't have permission to acknowledge this alert.");
        }

        return ResponseEntity.ok(alertService.acknowledge(id));
    }
}