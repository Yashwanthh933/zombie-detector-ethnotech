package com.zombiedetector.repository;

import com.zombiedetector.model.ManagedNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<ManagedNode, String> {
}