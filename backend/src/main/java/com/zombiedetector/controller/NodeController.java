package com.zombiedetector.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.dto.CreateNodeRequest;
import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.model.Role;
import com.zombiedetector.model.User;
import com.zombiedetector.repository.NodeRepository;
import com.zombiedetector.service.AuditService;
import com.zombiedetector.service.NodeService;
import com.zombiedetector.service.SavingsService;

@RestController
public class NodeController {

    private static final Set<String> VALID_ENVIRONMENTS = Set.of("PROD", "STAGING", "DEV");
    private static final int MAX_NODES_PER_USER = 25;
    private static final int MAX_INSTANCE_TYPE_LENGTH = 40;
    private static final double MAX_HOURLY_RATE = 100.0;

    private final NodeRepository nodeRepository;
    private final AuditService auditService;
    private final NodeService nodeService;
    private final SavingsService savingsService;

    public NodeController(NodeRepository nodeRepository, AuditService auditService,
                           NodeService nodeService, SavingsService savingsService) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.nodeService = nodeService;
        this.savingsService = savingsService;
    }

    /** Admins see every node; regular users see only the ones they own. */
    private List<ManagedNode> scopeFor(User user) {
        return user.getRole() == Role.ADMIN
                ? nodeRepository.findAll()
                : nodeRepository.findByOwnerId(user.getId());
    }

    private static boolean canManage(User user, ManagedNode node) {
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = node.getOwnerId() != null && node.getOwnerId().equals(user.getId());
        return isAdmin || isOwner;
    }

    @GetMapping("/api/nodes")
    public ResponseEntity<?> getAllNodes(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        return ResponseEntity.ok(scopeFor(user));
    }

    @PostMapping("/api/nodes")
    public ResponseEntity<?> createNode(@RequestBody CreateNodeRequest request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required to add a resource.");
        }
        if (request == null || request.instanceType() == null || request.instanceType().isBlank()) {
            return ResponseEntity.badRequest().body("instanceType is required.");
        }
        String instanceType = request.instanceType().trim();
        if (instanceType.length() > MAX_INSTANCE_TYPE_LENGTH) {
            return ResponseEntity.badRequest().body("instanceType must be at most " + MAX_INSTANCE_TYPE_LENGTH + " characters.");
        }
        if (request.environment() == null || !VALID_ENVIRONMENTS.contains(request.environment().toUpperCase())) {
            return ResponseEntity.badRequest().body("environment must be one of PROD, STAGING, DEV.");
        }
        double rate = request.hourlyRate();
        if (Double.isNaN(rate) || rate < 0 || rate > MAX_HOURLY_RATE) {
            return ResponseEntity.badRequest().body("hourlyRate must be between 0 and " + (int) MAX_HOURLY_RATE + ".");
        }
        if (user.getRole() != Role.ADMIN && nodeRepository.countByOwnerId(user.getId()) >= MAX_NODES_PER_USER) {
            return ResponseEntity.badRequest().body("You can monitor at most " + MAX_NODES_PER_USER + " resources.");
        }

        String nodeId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String environment = request.environment().toUpperCase();

        ManagedNode node = new ManagedNode(nodeId, instanceType, rate, environment, user.getEmail());
        node.setOwnerId(user.getId());
        nodeRepository.save(node);

        auditService.log(user.getEmail(), "NODE_ADDED", nodeId, "SUCCESS",
                "User-added resource: " + instanceType + " (" + environment + ")");

        return ResponseEntity.ok(node);
    }

    /** Owner-or-admin only. Removes the node and its metric history. */
    @DeleteMapping("/api/nodes/{id}")
    public ResponseEntity<?> deleteNode(@PathVariable String id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        Optional<ManagedNode> maybeNode = nodeRepository.findById(id);
        if (maybeNode.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canManage(user, maybeNode.get())) {
            return ResponseEntity.status(403).body("You don't have permission to remove this node.");
        }

        nodeService.deleteNode(id);
        auditService.log(user.getEmail(), "NODE_DELETED", id, "SUCCESS", "Resource removed from monitoring");
        return ResponseEntity.ok(Map.of("deleted", id));
    }

    /** Admin sees savings across everything, a regular user sees only their own stopped nodes. */
    @GetMapping("/api/savings")
    public ResponseEntity<?> getSavings(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        List<ManagedNode> stopped = scopeFor(user).stream()
                .filter(n -> n.getStatus() == NodeStatus.STOPPED)
                .toList();

        double monthlySavings = stopped.stream()
                .mapToDouble(n -> n.getHourlyRate() * 24 * 30)
                .sum();

        return ResponseEntity.ok(Map.of(
                "stoppedCount", stopped.size(),
                "monthlySavings", Math.round(monthlySavings * 100.0) / 100.0
        ));
    }

    /** Real history, replayed from the audit trail (see SavingsService). */
    @GetMapping("/api/savings/history")
    public ResponseEntity<?> getSavingsHistory(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        return ResponseEntity.ok(savingsService.history(scopeFor(user)));
    }

    /** Owner-or-admin gated, and logs the real actor instead of a hardcoded name. */
    @PostMapping("/api/nodes/{id}/override")
    public ResponseEntity<?> overrideNode(@PathVariable String id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }

        Optional<ManagedNode> maybeNode = nodeRepository.findById(id);
        if (maybeNode.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canManage(user, maybeNode.get())) {
            return ResponseEntity.status(403).body("You don't have permission to override this node.");
        }

        Optional<ManagedNode> updated;
        try {
            updated = nodeService.applyOverride(id);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        }
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        auditService.log(user.getEmail(), "OVERRIDE", id, "SUCCESS", "Manual override, 24h shield applied");
        return ResponseEntity.ok(updated.get());
    }
}
