package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeMetric;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.repository.NodeMetricRepository;
import com.zombiedetector.repository.NodeRepository;

@Component
public class ZombieDetector {

    // Demo-speed constants -- see docs for the production-scale equivalents.
    private static final int IDLE_WINDOW_MINUTES = 2;   // stands in for "2+ hours" in production
    private static final int MIN_SAMPLES_REQUIRED = 5;  // don't judge on too little history
    private static final double CPU_THRESHOLD = 15.0;
    private static final int RECOVERY_STRIKES_REQUIRED = 3; // consecutive clean cycles before un-flagging
    private static final int GRACE_PERIOD_SECONDS = 20;     // stands in for ~24h in production

    private final NodeRepository nodeRepository;
    private final NodeMetricRepository metricRepository;

    public ZombieDetector(NodeRepository nodeRepository, NodeMetricRepository metricRepository) {
        this.nodeRepository = nodeRepository;
        this.metricRepository = metricRepository;
    }

    @Scheduled(fixedDelay = 30000)
    public void evaluateCluster() {
        System.out.println("DETECTOR TICK at " + LocalDateTime.now());
        List<ManagedNode> nodes = nodeRepository.findAll();

        for (ManagedNode node : nodes) {
            if (node.getStatus() == NodeStatus.STOPPED) continue;

            // Human override takes precedence over everything else.
            if (node.getManualOverrideUntil() != null
                    && LocalDateTime.now().isBefore(node.getManualOverrideUntil())) {
                continue;
            }

            Optional<String> reason = checkIfZombie(node);

            if (reason.isPresent() && node.getStatus() == NodeStatus.RUNNING) {
                node.setStatus(NodeStatus.FLAGGED);
                node.setFlaggedAt(LocalDateTime.now());
                node.setGracePeriodEndsAt(LocalDateTime.now().plusSeconds(GRACE_PERIOD_SECONDS));
                node.setCleanStreak(0);
                System.out.println("FLAGGED: " + node.getNodeId() + " - " + reason.get());

            } else if (node.getStatus() == NodeStatus.FLAGGED) {
                if (reason.isEmpty()) {
                    // Hysteresis: require several consecutive clean reads, not just one.
                    node.setCleanStreak(node.getCleanStreak() + 1);
                    if (node.getCleanStreak() >= RECOVERY_STRIKES_REQUIRED) {
                        node.setStatus(NodeStatus.RUNNING);
                        node.setFlaggedAt(null);
                        node.setGracePeriodEndsAt(null);
                        node.setCleanStreak(0);
                        System.out.println("RECOVERED: " + node.getNodeId());
                    } else {
                        System.out.println("RECOVERING (streak " + node.getCleanStreak() + "/"
                                + RECOVERY_STRIKES_REQUIRED + "): " + node.getNodeId());
                    }
                } else {
                    node.setCleanStreak(0); // still idle -- reset any partial recovery progress

                    if (LocalDateTime.now().isAfter(node.getGracePeriodEndsAt())) {
                        if (node.getEnvironment().equals("PROD")) {
                            System.out.println("HOLDING (PROD, needs manual review): " + node.getNodeId());
                        } else {
                            node.setStatus(NodeStatus.STOPPED);
                            double monthlySavings = node.getHourlyRate() * 24 * 30;
                            System.out.printf("STOPPED: %s - Monthly savings: $%.2f%n",
                                    node.getNodeId(), monthlySavings);
                        }
                    }
                }
            }
        }
        nodeRepository.saveAll(nodes);
    }

    /**
     * Derives idleness from ACTUAL PERSISTED HISTORY, not a single cached value.
     * Requires a minimum number of samples so a brand-new node (or one with a
     * gap in metrics) is never judged on insufficient data.
     */
    private Optional<String> checkIfZombie(ManagedNode node) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(IDLE_WINDOW_MINUTES);
        List<NodeMetric> recent = metricRepository.findByNodeIdAndTimestampAfter(node.getNodeId(), windowStart);
        return ZombieRules.checkIfZombie(recent, CPU_THRESHOLD, MIN_SAMPLES_REQUIRED, IDLE_WINDOW_MINUTES);
    }
}