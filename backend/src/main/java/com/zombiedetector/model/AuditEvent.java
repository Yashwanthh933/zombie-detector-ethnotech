package com.zombiedetector.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String actor;
    private String action;
    private String targetNodeId;
    private String outcome;
    private String details;

    public AuditEvent() {}

    public AuditEvent(String actor, String action, String targetNodeId, String outcome, String details) {
        this.timestamp = LocalDateTime.now();
        this.actor = actor;
        this.action = action;
        this.targetNodeId = targetNodeId;
        this.outcome = outcome;
        this.details = details;
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTargetNodeId() { return targetNodeId; }
    public String getOutcome() { return outcome; }
    public String getDetails() { return details; }
}