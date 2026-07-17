package com.zombiedetector.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class NodeMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nodeId;
    private LocalDateTime timestamp;
    private double cpuLoad;
    private boolean hadTraffic;

    public NodeMetric() {}

    public NodeMetric(String nodeId, double cpuLoad, boolean hadTraffic) {
        this.nodeId = nodeId;
        this.cpuLoad = cpuLoad;
        this.hadTraffic = hadTraffic;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNodeId() { return nodeId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getCpuLoad() { return cpuLoad; }
    public boolean isHadTraffic() { return hadTraffic; }
}