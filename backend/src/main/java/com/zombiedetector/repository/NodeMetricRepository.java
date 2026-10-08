package com.zombiedetector.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zombiedetector.model.NodeMetric;

public interface NodeMetricRepository extends JpaRepository<NodeMetric, Long> {
    List<NodeMetric> findByNodeIdAndTimestampAfter(String nodeId, LocalDateTime cutoff);
    long deleteByTimestampBefore(LocalDateTime cutoff);
    long deleteByNodeId(String nodeId);
}