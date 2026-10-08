package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeMetric;
import com.zombiedetector.repository.NodeMetricRepository;
import com.zombiedetector.repository.NodeRepository;

@Component
public class TrafficSimulator {

    // Rotates which nodes are "idle-natured" every N ticks, instead of a static
    // hash-based assignment -- otherwise the exact same nodes are idle forever
    // and the demo goes flat after one cycle.
    private static final int ROTATE_EVERY_N_TICKS = 18; // ~3 min at 10s ticks

    private final NodeRepository nodeRepository;
    private final NodeMetricRepository metricRepository;
    private final SchedulerState schedulerState;
    private final Random rand = new Random();

    private Set<String> idleNodeIds = new HashSet<>();
    private int tickCounter = 0;

    public TrafficSimulator(NodeRepository nodeRepository, NodeMetricRepository metricRepository,
                             SchedulerState schedulerState) {
        this.nodeRepository = nodeRepository;
        this.metricRepository = metricRepository;
        this.schedulerState = schedulerState;
    }

    @Scheduled(fixedDelay = 10000)
    public void simulate() {
        if (schedulerState.isPaused()) {
            System.out.println("SIMULATOR PAUSED - skipping tick");
            return;
        }

        tickCounter++;
        List<ManagedNode> nodes = nodeRepository.findAll();

        if (idleNodeIds.isEmpty() || tickCounter % ROTATE_EVERY_N_TICKS == 0) {
            rotateIdleProfile(nodes);
        }

        for (ManagedNode snapshot : nodes) {
            // Re-read each node so we never write back stale status/override fields, and skip
            // nodes that were removed since the tick began.
            Optional<ManagedNode> fresh = nodeRepository.findById(snapshot.getNodeId());
            if (fresh.isEmpty()) continue;
            ManagedNode node = fresh.get();

            boolean idleProfile = idleNodeIds.contains(node.getNodeId());

            double cpu = idleProfile ? rand.nextDouble() * 10 : 15 + rand.nextDouble() * 70;
            boolean hadTraffic = idleProfile ? rand.nextInt(100) < 5 : rand.nextInt(100) < 70;

            node.setAvgCpuLoadLast15Min(cpu);
            node.setLastTrafficTimestamp(hadTraffic ? LocalDateTime.now() : node.getLastTrafficTimestamp());
            try {
                nodeRepository.save(node);
                metricRepository.save(new NodeMetric(node.getNodeId(), cpu, hadTraffic));
            } catch (OptimisticLockingFailureException ex) {
                // A user action or the detector touched this node mid-tick; skip one sample.
            }
        }
    }

    private void rotateIdleProfile(List<ManagedNode> nodes) {
        idleNodeIds = new HashSet<>();
        for (ManagedNode node : nodes) {
            if (rand.nextInt(100) < 30) idleNodeIds.add(node.getNodeId());
        }
        System.out.println("SIMULATOR: rotated idle profile, " + idleNodeIds.size() + " nodes now idle-natured");
    }
}