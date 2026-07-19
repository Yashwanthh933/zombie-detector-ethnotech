package com.zombiedetector.service;

import java.util.List;
import java.util.Optional;

import com.zombiedetector.model.NodeMetric;

/** Pure decision logic, deliberately separated from Spring/DB so it's unit-testable in isolation. */
public class ZombieRules {

    public static Optional<String> checkIfZombie(List<NodeMetric> recentMetrics, double cpuThreshold,
                                                   int minSamples, int windowMinutes) {
        if (recentMetrics.size() < minSamples) {
            return Optional.empty();
        }

        double maxCpu = recentMetrics.stream().mapToDouble(NodeMetric::getCpuLoad).max().orElse(100);
        boolean anyTraffic = recentMetrics.stream().anyMatch(NodeMetric::isHadTraffic);

        if (maxCpu < cpuThreshold && !anyTraffic) {
            return Optional.of("Idle: CPU stayed below " + cpuThreshold + "% and zero traffic across "
                    + recentMetrics.size() + " samples over the last " + windowMinutes + " min");
        }
        return Optional.empty();
    }
}