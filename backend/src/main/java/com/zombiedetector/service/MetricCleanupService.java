package com.zombiedetector.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zombiedetector.repository.NodeMetricRepository;

@Component
public class MetricCleanupService {

    // Demo-scale stand-in for a real "delete anything older than 30 days" job.
    private static final int RETENTION_MINUTES = 10;

    private final NodeMetricRepository metricRepository;

    public MetricCleanupService(NodeMetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    @Scheduled(fixedDelay = 60000) // every 1 minute
    @Transactional
    public void cleanupOldMetrics() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RETENTION_MINUTES);
        long deleted = metricRepository.deleteByTimestampBefore(cutoff);
        if (deleted > 0) {
            System.out.println("CLEANUP: removed " + deleted + " metric rows older than " + RETENTION_MINUTES + " min");
        }
    }
}