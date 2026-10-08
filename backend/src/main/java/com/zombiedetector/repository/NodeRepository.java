package com.zombiedetector.repository;

import java.util.List;

import com.zombiedetector.model.ManagedNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<ManagedNode, String> {
    List<ManagedNode> findByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
}