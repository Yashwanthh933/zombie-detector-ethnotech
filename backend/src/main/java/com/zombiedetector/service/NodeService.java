package com.zombiedetector.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.model.NodeStatus;
import com.zombiedetector.repository.NodeMetricRepository;
import com.zombiedetector.repository.NodeRepository;

@Service
public class NodeService {

    private static final int OVERRIDE_ATTEMPTS = 3;

    private final NodeRepository nodeRepository;
    private final NodeMetricRepository metricRepository;

    public NodeService(NodeRepository nodeRepository, NodeMetricRepository metricRepository) {
        this.nodeRepository = nodeRepository;
        this.metricRepository = metricRepository;
    }

    /**
     * Returns the node to RUNNING with a 24h shield. The simulator also writes this row every
     * few seconds, so a concurrent-write conflict is retried rather than surfaced as an error.
     *
     * @return the updated node, or empty if it no longer exists
     */
    public Optional<ManagedNode> applyOverride(String nodeId) {
        for (int attempt = 0; attempt < OVERRIDE_ATTEMPTS; attempt++) {
            Optional<ManagedNode> maybe = nodeRepository.findById(nodeId);
            if (maybe.isEmpty()) return Optional.empty();

            ManagedNode node = maybe.get();
            node.setStatus(NodeStatus.RUNNING);
            node.setFlaggedAt(null);
            node.setGracePeriodEndsAt(null);
            node.setCleanStreak(0);
            node.setManualOverrideUntil(LocalDateTime.now().plusHours(24));
            try {
                return Optional.of(nodeRepository.save(node));
            } catch (OptimisticLockingFailureException ex) {
                // Someone else wrote the row between our read and write; re-read and try again.
            }
        }
        throw new IllegalStateException("The node was being updated; please try again.");
    }

    /** Removes the node and its metric history together. */
    @Transactional
    public void deleteNode(String nodeId) {
        metricRepository.deleteByNodeId(nodeId);
        nodeRepository.deleteById(nodeId);
    }
}
