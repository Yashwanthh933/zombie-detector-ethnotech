package com.zombiedetector.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zombiedetector.model.AuditEvent;
import com.zombiedetector.model.ManagedNode;

class SavingsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    private ManagedNode node(String id, double hourlyRate) {
        return new ManagedNode(id, "t2.micro", hourlyRate, "DEV", "owner@example.com");
    }

    private AuditEvent event(int month, int day, int hour, String action, String nodeId) {
        return new AuditEvent(LocalDateTime.of(2026, month, day, hour, 0), "scheduler", action, nodeId, "SUCCESS", "");
    }

    @Test
    void replaysTheAuditTrailDayByDay() {
        List<ManagedNode> scope = List.of(node("node-1", 1.0), node("node-2", 0.5));
        List<AuditEvent> events = List.of(
                event(10, 6, 10, "FLAGGED", "node-1"),
                event(10, 7, 9, "STOPPED", "node-1"),
                event(10, 7, 12, "FLAGGED", "node-2"),
                event(10, 7, 13, "STOPPED", "not-in-scope"),   // other tenant's node: must be ignored
                event(10, 7, 14, "POLICY_UPDATED", null),      // not tied to a node: must be ignored
                event(10, 8, 8, "OVERRIDE", "node-2"));

        List<Map<String, Object>> history = SavingsService.computeHistory(scope, events, TODAY, 3);

        assertEquals(3, history.size());

        // Oct 6: node-1 flagged but still running -> waste of 1.0 * 24
        assertEquals("2026-10-06", history.get(0).get("date"));
        assertEquals(0.0, (Double) history.get(0).get("savings"), 0.001);
        assertEquals(24.0, (Double) history.get(0).get("waste"), 0.001);

        // Oct 7: node-1 stopped (saves 24/day), node-2 newly flagged (waste 0.5 * 24)
        assertEquals(24.0, (Double) history.get(1).get("savings"), 0.001);
        assertEquals(12.0, (Double) history.get(1).get("waste"), 0.001);

        // Oct 8: node-2 overridden back to running, so only node-1's savings remain
        assertEquals(24.0, (Double) history.get(2).get("savings"), 0.001);
        assertEquals(0.0, (Double) history.get(2).get("waste"), 0.001);
    }

    @Test
    void noEventsMeansFlatZeros() {
        List<Map<String, Object>> history =
                SavingsService.computeHistory(List.of(node("node-1", 1.0)), List.of(), TODAY, 7);

        assertEquals(7, history.size());
        for (Map<String, Object> row : history) {
            assertEquals(0.0, (Double) row.get("savings"), 0.001);
            assertEquals(0.0, (Double) row.get("waste"), 0.001);
        }
    }

    @Test
    void eventsBeforeTheWindowStillSetTheStartingState() {
        List<ManagedNode> scope = List.of(node("node-1", 1.0));
        List<AuditEvent> events = List.of(event(9, 1, 9, "STOPPED", "node-1")); // stopped well before the 7-day window

        List<Map<String, Object>> history = SavingsService.computeHistory(scope, events, TODAY, 7);

        assertEquals(24.0, (Double) history.get(0).get("savings"), 0.001);
        assertEquals(24.0, (Double) history.get(6).get("savings"), 0.001);
    }
}
