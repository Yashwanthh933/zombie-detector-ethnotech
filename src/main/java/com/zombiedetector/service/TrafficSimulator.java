package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeMetric;
import com.zombiedetector.repository.NodeMetricRepository;
import com.zombiedetector.repository.NodeRepository;

@Component
public class TrafficSimulator {

    private final NodeRepository nodeRepository;
    private final NodeMetricRepository metricRepository;
    private final Random rand = new Random();

    public TrafficSimulator(NodeRepository nodeRepository, NodeMetricRepository metricRepository) {
        this.nodeRepository = nodeRepository;
        this.metricRepository = metricRepository;
    }

    @Scheduled(fixedDelay = 10000)
    public void simulate() {
        System.out.println("SIMULATOR TICK at " + LocalDateTime.now());
        List<ManagedNode> nodes = nodeRepository.findAll();

        for (ManagedNode node : nodes) {
            // ~1/3 of nodes behave as naturally idle-natured (mirrors real fleets:
            // some servers genuinely are quiet). This is a usage PROFILE, not a
            // pre-written verdict -- the detector still has to derive idleness
            // itself by aggregating raw samples over a time window.
            boolean idleProfile = Math.floorMod(node.getNodeId().hashCode(), 3) == 0;

            double cpu = idleProfile ? rand.nextDouble() * 10 : 15 + rand.nextDouble() * 70;
            boolean hadTraffic = idleProfile ? rand.nextInt(100) < 5 : rand.nextInt(100) < 70;

            metricRepository.save(new NodeMetric(node.getNodeId(), cpu, hadTraffic));

            // cosmetic only -- these fields on ManagedNode are for the /api/nodes
            // display, NOT read by the detector. Kept so the API still shows
            // "current" values without forcing every consumer to query history.
            node.setAvgCpuLoadLast15Min(cpu);
            node.setLastTrafficTimestamp(hadTraffic ? LocalDateTime.now() : node.getLastTrafficTimestamp());
        }
        nodeRepository.saveAll(nodes);
    }
}