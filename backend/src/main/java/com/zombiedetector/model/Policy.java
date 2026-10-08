package com.zombiedetector.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Policy {
    @Id
    private Long id = 1L;

    private double cpuThreshold = 15.0;
    private int idleWindowMinutes = 2;
    private int minSamplesRequired = 5;
    private int recoveryStrikesRequired = 3;
    private int gracePeriodSeconds = 20;

    private LocalDateTime updatedAt;
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getCpuThreshold() { return cpuThreshold; }
    public void setCpuThreshold(double v) { this.cpuThreshold = v; }
    public int getIdleWindowMinutes() { return idleWindowMinutes; }
    public void setIdleWindowMinutes(int v) { this.idleWindowMinutes = v; }
    public int getMinSamplesRequired() { return minSamplesRequired; }
    public void setMinSamplesRequired(int v) { this.minSamplesRequired = v; }
    public int getRecoveryStrikesRequired() { return recoveryStrikesRequired; }
    public void setRecoveryStrikesRequired(int v) { this.recoveryStrikesRequired = v; }
    public int getGracePeriodSeconds() { return gracePeriodSeconds; }
    public void setGracePeriodSeconds(int v) { this.gracePeriodSeconds = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}