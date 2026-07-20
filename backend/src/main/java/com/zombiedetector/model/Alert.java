package com.zombiedetector.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String severity; // INFO, WARNING, CRITICAL
    private String title;
    private String message;
    private String targetNodeId;
    private boolean acknowledged;

    public Alert() {}

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String id) { this.targetNodeId = id; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean a) { this.acknowledged = a; }
}