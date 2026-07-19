package com.zombiedetector.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.model.Policy;
import com.zombiedetector.service.PolicyService;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public Policy getPolicy() {
        return policyService.getCurrentPolicy();
    }

    @PutMapping
    public Policy updatePolicy(@RequestBody Policy policy) {
        return policyService.updatePolicy(policy);
    }
}