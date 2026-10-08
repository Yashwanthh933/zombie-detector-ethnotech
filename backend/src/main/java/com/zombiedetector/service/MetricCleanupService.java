package com.zombiedetector.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zombiedetector.repository.NodeMetricRepository;

@Component
public class MetricCleanupService {

    // Floor for the demo-scale stand-in for a real "delete anything older than 30 days" job.
    private static final int MIN_RETENTION_MINUTES = 10;
    // Keep a little more than the idle window so the detector always has a full window to read.
    private static final int RETENTION_BUFFER_MINUTES = 5;

    private final NodeMetricRepository metricRepository;
    private final PolicyService policyService;

    public MetricCleanupService(NodeMetricRepository metricRepository, PolicyService policyService) {
        this.metricRepository = metricRepository;
        this.policyService = policyService;
    }

    /** Retention follows the policy: it must never be shorter than the idle window being evaluated. */
    static int retentionMinutes(int idleWindowMinutes) {
        return Math.max(MIN_RETENTION_MINUTES, idleWindowMinutes + RETENTION_BUFFER_MINUTES);
    }

    @Scheduled(fixedDelay = 60000) // every 1 minute
    @Transactional
    public void cleanupOldMetrics() {
        int retention = retentionMinutes(policyService.getCurrentPolicy().getIdleWindowMinutes());
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(retention);
        long deleted = metricRepository.deleteByTimestampBefore(cutoff);
        if (deleted > 0) {
            System.out.println("CLEANUP: removed " + deleted + " metric rows older than " + retention + " min");
        }
    }
}
