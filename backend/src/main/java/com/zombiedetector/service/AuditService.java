package com.zombiedetector.service;

import org.springframework.stereotype.Service;

import com.zombiedetector.model.AuditEvent;
import com.zombiedetector.repository.AuditEventRepository;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void log(String actor, String action, String targetNodeId, String outcome, String details) {
        auditEventRepository.save(new AuditEvent(actor, action, targetNodeId, outcome, details));
    }
}