package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.repository.NodeRepository;

@Component
public class ZombieDetector {

    private final NodeRepository nodeRepository;

    public ZombieDetector(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Scheduled(fixedDelay = 30000) // every 30s for demo speed
    public void evaluateCluster() {
        System.out.println("DETECTOR TICK at " + LocalDateTime.now());
        List<ManagedNode> nodes = nodeRepository.findAll();

        for (ManagedNode node : nodes) {
            if (node.getStatus() == NodeStatus.STOPPED) continue;

            Optional<String> reason = checkIfZombie(node);

            if (reason.isPresent() && node.getStatus() == NodeStatus.RUNNING) {
                node.setStatus(NodeStatus.FLAGGED);
                node.setFlaggedAt(LocalDateTime.now());
                node.setGracePeriodEndsAt(LocalDateTime.now().plusSeconds(20));
                System.out.println("FLAGGED: " + node.getNodeId() + " - " + reason.get());

            } else if (node.getStatus() == NodeStatus.FLAGGED) {
                if (reason.isEmpty()) {
                    node.setStatus(NodeStatus.RUNNING);
                    node.setFlaggedAt(null);
                    node.setGracePeriodEndsAt(null);
                    System.out.println("RECOVERED: " + node.getNodeId());
                } else if (LocalDateTime.now().isAfter(node.getGracePeriodEndsAt())) {
                    node.setStatus(NodeStatus.STOPPED);
                    double monthlySavings = node.getHourlyRate() * 24 * 30;
                    System.out.printf("STOPPED: %s - Monthly savings: $%.2f%n", node.getNodeId(), monthlySavings);
                }
            }
        }
        nodeRepository.saveAll(nodes);
    }

    private Optional<String> checkIfZombie(ManagedNode node) {
        boolean cpuIsLow = node.getAvgCpuLoadLast15Min() < 15;
        boolean noRecentTraffic = node.getLastTrafficTimestamp()
                .isBefore(LocalDateTime.now().minusHours(2));

        if (cpuIsLow && noRecentTraffic) {
            return Optional.of("Idle: low CPU + no traffic for 2+ hours");
        }
        return Optional.empty();
    }
}