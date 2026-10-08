package com.zombiedetector.service;

import java.time.LocalDateTime;

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

    /**
     * Validates, then copies only the five tunable fields onto the singleton row, so a client
     * can never set id/updatedAt/updatedBy itself.
     *
     * @throws IllegalArgumentException with a user-facing message if the values are out of bounds
     */
    public Policy updatePolicy(Policy incoming, String updatedBy) {
        PolicyValidator.validate(incoming).ifPresent(message -> {
            throw new IllegalArgumentException(message);
        });

        Policy current = getCurrentPolicy();
        current.setCpuThreshold(incoming.getCpuThreshold());
        current.setIdleWindowMinutes(incoming.getIdleWindowMinutes());
        current.setMinSamplesRequired(incoming.getMinSamplesRequired());
        current.setRecoveryStrikesRequired(incoming.getRecoveryStrikesRequired());
        current.setGracePeriodSeconds(incoming.getGracePeriodSeconds());
        current.setUpdatedAt(LocalDateTime.now());
        current.setUpdatedBy(updatedBy);
        return policyRepository.save(current);
    }
}
