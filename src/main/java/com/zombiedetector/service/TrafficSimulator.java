package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.repository.NodeRepository;

@Component
public class TrafficSimulator {

    private final NodeRepository nodeRepository;
    private final Random rand = new Random();

    public TrafficSimulator(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Scheduled(fixedDelay = 10000) // every 10 seconds
    @Scheduled(fixedDelay = 10000) // every 10 seconds
    public void simulate() {
        System.out.println("SIMULATOR TICK at " + LocalDateTime.now());
        List<ManagedNode> nodes = nodeRepository.findAll();

        for (ManagedNode node : nodes) {
            boolean isQuietThisCycle = rand.nextInt(100) < 35; // 35% chance this node "looks abandoned"

            if (isQuietThisCycle) {
                node.setAvgCpuLoadLast15Min(rand.nextDouble() * 10);          // low CPU
                node.setLastTrafficTimestamp(LocalDateTime.now().minusHours(3)); // AND stale traffic, together
            } else {
                node.setAvgCpuLoadLast15Min(10 + rand.nextDouble() * 70);     // active CPU (never dips below 10)
                node.setLastTrafficTimestamp(LocalDateTime.now());            // fresh traffic
            }
        }
        nodeRepository.saveAll(nodes);
    }
}
