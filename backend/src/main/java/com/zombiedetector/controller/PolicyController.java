package com.zombiedetector.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zombiedetector.model.Policy;
import com.zombiedetector.model.User;
import com.zombiedetector.service.AuditService;
import com.zombiedetector.service.PolicyNotificationService;
import com.zombiedetector.service.PolicyService;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyNotificationService policyNotificationService;
    private final AuditService auditService;

    public PolicyController(PolicyService policyService, PolicyNotificationService policyNotificationService,
                             AuditService auditService) {
        this.policyService = policyService;
        this.policyNotificationService = policyNotificationService;
        this.auditService = auditService;
    }

    @GetMapping
    public Policy getPolicy() {
        return policyService.getCurrentPolicy();
    }

    /** Admin-only: this is the "admin changes policy" requirement. Validated, audited, then broadcast to every client. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePolicy(@RequestBody Policy policy, @AuthenticationPrincipal User admin) {
        Policy updated;
        try {
            updated = policyService.updatePolicy(policy, admin.getEmail());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        auditService.log(admin.getEmail(), "POLICY_UPDATED", null, "SUCCESS", String.format(
                "cpu<%.1f%%, window=%dmin, minSamples=%d, recoveryStrikes=%d, grace=%ds",
                updated.getCpuThreshold(), updated.getIdleWindowMinutes(), updated.getMinSamplesRequired(),
                updated.getRecoveryStrikesRequired(), updated.getGracePeriodSeconds()));

        policyNotificationService.broadcastPolicyUpdated(updated);
        return ResponseEntity.ok(updated);
    }

    /**
     * SSE stream -- any authenticated client (not just admins) subscribes here to get
     * notified the moment an admin changes policy. Auth comes via ?token= since EventSource
     * can't set custom headers (see JwtAuthenticationFilter.resolveToken).
     */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return policyNotificationService.subscribe();
    }
}
