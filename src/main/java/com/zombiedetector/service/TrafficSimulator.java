package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

        for (ManagedNode node : nodes) {
            boolean idleProfile = idleNodeIds.contains(node.getNodeId());

            double cpu = idleProfile ? rand.nextDouble() * 10 : 15 + rand.nextDouble() * 70;
            boolean hadTraffic = idleProfile ? rand.nextInt(100) < 5 : rand.nextInt(100) < 70;

            metricRepository.save(new NodeMetric(node.getNodeId(), cpu, hadTraffic));

            node.setAvgCpuLoadLast15Min(cpu);
            node.setLastTrafficTimestamp(hadTraffic ? LocalDateTime.now() : node.getLastTrafficTimestamp());
        }
        nodeRepository.saveAll(nodes);
    }

    private void rotateIdleProfile(List<ManagedNode> nodes) {
        idleNodeIds = new HashSet<>();
        for (ManagedNode node : nodes) {
            if (rand.nextInt(100) < 30) idleNodeIds.add(node.getNodeId());
        }
        System.out.println("SIMULATOR: rotated idle profile, " + idleNodeIds.size() + " nodes now idle-natured");
    }
}