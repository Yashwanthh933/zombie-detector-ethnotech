package com.zombiedetector.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.zombiedetector.model.AuditEvent;
import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.repository.AuditEventRepository;

/**
 * Builds the dashboard's savings chart from what actually happened, by replaying the audit
 * trail (FLAGGED / STOPPED / RECOVERED / OVERRIDE) day by day -- not from extrapolating today's number.
 */
@Service
public class SavingsService {

    private final AuditEventRepository auditEventRepository;

    public SavingsService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public List<Map<String, Object>> history(List<ManagedNode> scope) {
        List<AuditEvent> newestFirst = auditEventRepository.findAllByOrderByTimestampDesc();
        List<AuditEvent> oldestFirst = new ArrayList<>(newestFirst);
        Collections.reverse(oldestFirst);
        return computeHistory(scope, oldestFirst, LocalDate.now(), 7);
    }

    /**
     * For each of the last {@code days} days, the daily cost (USD/day) of nodes that were
     * STOPPED at the end of that day ("savings") and of nodes that were FLAGGED but still
     * running ("waste"), according to the audit events up to that point.
     */
    static List<Map<String, Object>> computeHistory(List<ManagedNode> scope, List<AuditEvent> eventsOldestFirst,
                                                    LocalDate today, int days) {
        Map<String, ManagedNode> byId = new HashMap<>();
        for (ManagedNode n : scope) byId.put(n.getNodeId(), n);

        Map<String, NodeStatus> state = new HashMap<>();
        List<Map<String, Object>> out = new ArrayList<>();
        int next = 0;

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();

            while (next < eventsOldestFirst.size() && eventsOldestFirst.get(next).getTimestamp().isBefore(endOfDay)) {
                AuditEvent e = eventsOldestFirst.get(next++);
                String nodeId = e.getTargetNodeId();
                if (nodeId == null || !byId.containsKey(nodeId) || e.getAction() == null) continue;

                switch (e.getAction()) {
                    case "FLAGGED" -> state.put(nodeId, NodeStatus.FLAGGED);
                    case "STOPPED" -> state.put(nodeId, NodeStatus.STOPPED);
                    case "RECOVERED", "OVERRIDE" -> state.put(nodeId, NodeStatus.RUNNING);
                    default -> { /* NODE_ADDED, POLICY_UPDATED, ... don't change a node's state */ }
                }
            }

            double savings = 0;
            double waste = 0;
            for (Map.Entry<String, NodeStatus> entry : state.entrySet()) {
                double dailyCost = byId.get(entry.getKey()).getHourlyRate() * 24;
                if (entry.getValue() == NodeStatus.STOPPED) savings += dailyCost;
                else if (entry.getValue() == NodeStatus.FLAGGED) waste += dailyCost;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("date", day.toString());
            row.put("savings", Math.round(savings * 100.0) / 100.0);
            row.put("waste", Math.round(waste * 100.0) / 100.0);
            out.add(row);
        }
        return out;
    }
}
