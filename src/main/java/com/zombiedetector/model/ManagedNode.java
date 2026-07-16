package com.zombiedetector.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class ManagedNode {

    @Id
    private String nodeId;

    private String instanceType;
    private double hourlyRate;
    private String environment;

    private double avgCpuLoadLast15Min;
    private LocalDateTime lastTrafficTimestamp;

    private NodeStatus status;
    private LocalDateTime flaggedAt;
    private LocalDateTime gracePeriodEndsAt;

    public ManagedNode() {} // required by JPA

    public ManagedNode(String nodeId, String instanceType, double hourlyRate, String environment) {
        this.nodeId = nodeId;
        this.instanceType = instanceType;
        this.hourlyRate = hourlyRate;
        this.environment = environment;
        this.status = NodeStatus.RUNNING;
        this.lastTrafficTimestamp = LocalDateTime.now();
    }

    public String getNodeId() { return nodeId; }
    public String getInstanceType() { return instanceType; }
    public double getHourlyRate() { return hourlyRate; }
    public String getEnvironment() { return environment; }

    public double getAvgCpuLoadLast15Min() { return avgCpuLoadLast15Min; }
    public void setAvgCpuLoadLast15Min(double v) { this.avgCpuLoadLast15Min = v; }

    public LocalDateTime getLastTrafficTimestamp() { return lastTrafficTimestamp; }
    public void setLastTrafficTimestamp(LocalDateTime t) { this.lastTrafficTimestamp = t; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public LocalDateTime getFlaggedAt() { return flaggedAt; }
    public void setFlaggedAt(LocalDateTime t) { this.flaggedAt = t; }

    public LocalDateTime getGracePeriodEndsAt() { return gracePeriodEndsAt; }
    public void setGracePeriodEndsAt(LocalDateTime t) { this.gracePeriodEndsAt = t; }
}