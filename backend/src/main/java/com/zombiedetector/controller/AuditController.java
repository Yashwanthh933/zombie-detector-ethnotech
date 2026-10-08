package com.zombiedetector.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.AuditEvent;
import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.Role;
import com.zombiedetector.model.User;
import com.zombiedetector.repository.AuditEventRepository;
import com.zombiedetector.repository.NodeRepository;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final NodeRepository nodeRepository;

    public AuditController(AuditEventRepository auditEventRepository, NodeRepository nodeRepository) {
        this.auditEventRepository = auditEventRepository;
        this.nodeRepository = nodeRepository;
    }

    /** Same scoping approach as AlertController: admins see everything, users see only their own nodes' events. */
    @GetMapping
    public ResponseEntity<?> getAuditTrail(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        List<AuditEvent> all = auditEventRepository.findAllByOrderByTimestampDesc();

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(all);
        }

        Set<String> ownedNodeIds = nodeRepository.findByOwnerId(user.getId()).stream()
                .map(ManagedNode::getNodeId)
                .collect(Collectors.toSet());

        List<AuditEvent> scoped = all.stream()
                .filter(e -> ownedNodeIds.contains(e.getTargetNodeId()))
                .toList();

        return ResponseEntity.ok(scoped);
    }
}