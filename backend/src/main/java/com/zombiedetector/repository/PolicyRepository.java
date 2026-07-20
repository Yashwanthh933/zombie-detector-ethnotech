package com.zombiedetector.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zombiedetector.model.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}