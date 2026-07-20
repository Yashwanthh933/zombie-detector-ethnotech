package com.zombiedetector.service;

import org.springframework.stereotype.Service;

import com.zombiedetector.model.Policy;
import com.zombiedetector.repository.PolicyRepository;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public Policy getCurrentPolicy() {
        return policyRepository.findById(1L).orElseGet(() -> policyRepository.save(new Policy()));
    }

    public Policy updatePolicy(Policy updated) {
        updated.setId(1L);
        return policyRepository.save(updated);
    }
}