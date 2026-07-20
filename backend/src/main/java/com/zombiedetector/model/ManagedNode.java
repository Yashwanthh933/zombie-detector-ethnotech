package com.zombiedetector.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ManagedNode {

    @Id
    private String nodeId;

    private String instanceType;
    private double hourlyRate;
    private String environment;
    private String ownerEmail;

    private double avgCpuLoadLast15Min;
    private LocalDateTime lastTrafficTimestamp;

    private NodeStatus status;
    private LocalDateTime flaggedAt;
    private LocalDateTime gracePeriodEndsAt;
    private int cleanStreak;
    private LocalDateTime manualOverrideUntil;

    public ManagedNode() {}

    public ManagedNode(String nodeId, String instanceType, double hourlyRate, String environment, String ownerEmail) {
        this.nodeId = nodeId;
        this.instanceType = instanceType;
        this.hourlyRate = hourlyRate;
        this.environment = environment;
        this.ownerEmail = ownerEmail;
        this.status = NodeStatus.RUNNING;
        this.lastTrafficTimestamp = LocalDateTime.now();
        this.cleanStreak = 0;
    }

    public String getNodeId() { return nodeId; }
    public String getInstanceType() { return instanceType; }
    public double getHourlyRate() { return hourlyRate; }
    public String getEnvironment() { return environment; }
    public String getOwnerEmail() { return ownerEmail; }

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

    public int getCleanStreak() { return cleanStreak; }
    public void setCleanStreak(int v) { this.cleanStreak = v; }

    public LocalDateTime getManualOverrideUntil() { return manualOverrideUntil; }
    public void setManualOverrideUntil(LocalDateTime t) { this.manualOverrideUntil = t; }
}