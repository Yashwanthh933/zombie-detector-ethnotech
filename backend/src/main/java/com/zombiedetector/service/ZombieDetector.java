package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeMetric;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.model.Policy;
import com.zombiedetector.repository.NodeMetricRepository;
import com.zombiedetector.repository.NodeRepository;

@Component
public class ZombieDetector {

    // How long a PROD node can sit HOLDING before we raise an alert about it (demo-scale).
    private static final int PROD_HOLD_ALERT_SECONDS = 60;

    private final NodeRepository nodeRepository;
    private final NodeMetricRepository metricRepository;
    private final PolicyService policyService;
    private final AuditService auditService;
    private final AlertService alertService;
    private final SchedulerState schedulerState;

    public ZombieDetector(NodeRepository nodeRepository, NodeMetricRepository metricRepository,
                           PolicyService policyService, AuditService auditService,
                           AlertService alertService, SchedulerState schedulerState) {
        this.nodeRepository = nodeRepository;
        this.metricRepository = metricRepository;
        this.policyService = policyService;
        this.auditService = auditService;
        this.alertService = alertService;
        this.schedulerState = schedulerState;
    }

    @Scheduled(fixedDelay = 30000)
    public void evaluateCluster() {
        if (schedulerState.isPaused()) {
            System.out.println("DETECTOR PAUSED - skipping tick");
            return;
        }

        System.out.println("DETECTOR TICK at " + LocalDateTime.now());
        Policy policy = policyService.getCurrentPolicy();
        List<ManagedNode> nodes = nodeRepository.findAll();

        for (ManagedNode node : nodes) {
            if (node.getStatus() == NodeStatus.STOPPED) continue;

            if (node.getManualOverrideUntil() != null
                    && LocalDateTime.now().isBefore(node.getManualOverrideUntil())) {
                continue;
            }

            Optional<String> reason = checkIfZombie(node, policy);

            if (reason.isPresent() && node.getStatus() == NodeStatus.RUNNING) {
                node.setStatus(NodeStatus.FLAGGED);
                node.setFlaggedAt(LocalDateTime.now());
                node.setGracePeriodEndsAt(LocalDateTime.now().plusSeconds(policy.getGracePeriodSeconds()));
                node.setCleanStreak(0);
                System.out.println("FLAGGED: " + node.getNodeId() + " - " + reason.get());
                auditService.log("scheduler", "FLAGGED", node.getNodeId(), "SUCCESS", reason.get());

            } else if (node.getStatus() == NodeStatus.FLAGGED) {
                if (reason.isEmpty()) {
                    node.setCleanStreak(node.getCleanStreak() + 1);
                    if (node.getCleanStreak() >= policy.getRecoveryStrikesRequired()) {
                        node.setStatus(NodeStatus.RUNNING);
                        node.setFlaggedAt(null);
                        node.setGracePeriodEndsAt(null);
                        node.setCleanStreak(0);
                        System.out.println("RECOVERED: " + node.getNodeId());
                        auditService.log("scheduler", "RECOVERED", node.getNodeId(), "SUCCESS",
                                "Usage returned to normal");
                    } else {
                        System.out.println("RECOVERING (streak " + node.getCleanStreak() + "/"
                                + policy.getRecoveryStrikesRequired() + "): " + node.getNodeId());
                    }
                } else {
                    node.setCleanStreak(0);

                    if (LocalDateTime.now().isAfter(node.getGracePeriodEndsAt())) {
                        if (node.getEnvironment().equals("PROD")) {
                            System.out.println("HOLDING (PROD, needs manual review): " + node.getNodeId());

                            long heldSeconds = java.time.Duration.between(node.getFlaggedAt(), LocalDateTime.now()).getSeconds();
                            if (heldSeconds > PROD_HOLD_ALERT_SECONDS) {
                                alertService.raiseIfNotDuplicate("CRITICAL",
                                        "PROD node needs manual review",
                                        node.getNodeId() + " has been idle and held for over "
                                                + PROD_HOLD_ALERT_SECONDS + "s. Owner: " + node.getOwnerEmail(),
                                        node.getNodeId());
                            }
                        } else {
                            node.setStatus(NodeStatus.STOPPED);
                            double monthlySavings = node.getHourlyRate() * 24 * 30;
                            System.out.printf("STOPPED: %s - Monthly savings: $%.2f%n",
                                    node.getNodeId(), monthlySavings);
                            auditService.log("scheduler", "STOPPED", node.getNodeId(), "SUCCESS",
                                    String.format("Monthly savings: $%.2f, owner: %s", monthlySavings, node.getOwnerEmail()));
                            alertService.raiseIfNotDuplicate("INFO",
                                    "Cost optimization applied",
                                    node.getNodeId() + " stopped, saving $" + String.format("%.2f", monthlySavings)
                                            + "/mo. Owner: " + node.getOwnerEmail(),
                                    node.getNodeId());
                        }
                    }
                }
            }
        }
        nodeRepository.saveAll(nodes);
    }

    private Optional<String> checkIfZombie(ManagedNode node, Policy policy) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(policy.getIdleWindowMinutes());
        List<NodeMetric> recent = metricRepository.findByNodeIdAndTimestampAfter(node.getNodeId(), windowStart);
        return ZombieRules.checkIfZombie(recent, policy.getCpuThreshold(),
                policy.getMinSamplesRequired(), policy.getIdleWindowMinutes());
    }
}