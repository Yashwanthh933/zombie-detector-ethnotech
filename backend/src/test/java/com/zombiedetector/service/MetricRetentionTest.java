package com.zombiedetector.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MetricRetentionTest {

    @Test
    void retentionNeverFallsBelowTheFloor() {
        assertEquals(10, MetricCleanupService.retentionMinutes(1));
        assertEquals(10, MetricCleanupService.retentionMinutes(5));
    }

    @Test
    void retentionAlwaysCoversTheIdleWindowPlusBuffer() {
        for (int window = 1; window <= 120; window++) {
            assertTrue(MetricCleanupService.retentionMinutes(window) >= window + 5,
                    "retention must exceed idle window " + window);
        }
        assertEquals(125, MetricCleanupService.retentionMinutes(120));
    }
}
