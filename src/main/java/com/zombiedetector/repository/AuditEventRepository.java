package com.zombiedetector.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zombiedetector.model.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findAllByOrderByTimestampDesc();
}