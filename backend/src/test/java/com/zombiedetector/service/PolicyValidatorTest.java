package com.zombiedetector.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.zombiedetector.model.Policy;

class PolicyValidatorTest {

    private Policy valid() {
        return new Policy(); // defaults: 15%, 2 min, 5 samples, 3 strikes, 20 s
    }

    @Test
    void acceptsTheDefaultPolicy() {
        assertTrue(PolicyValidator.validate(valid()).isEmpty());
    }

    @Test
    void rejectsCpuThresholdOutOfRange() {
        Policy p = valid();
        p.setCpuThreshold(150);
        assertTrue(PolicyValidator.validate(p).isPresent());
        p.setCpuThreshold(-1);
        assertTrue(PolicyValidator.validate(p).isPresent());
        p.setCpuThreshold(Double.NaN);
        assertTrue(PolicyValidator.validate(p).isPresent());
    }

    @Test
    void rejectsZeroOrHugeIdleWindow() {
        Policy p = valid();
        p.setIdleWindowMinutes(0);
        assertTrue(PolicyValidator.validate(p).isPresent());
        p.setIdleWindowMinutes(121);
        assertTrue(PolicyValidator.validate(p).isPresent());
    }

    @Test
    void rejectsMinSamplesTheWindowCanNeverReach() {
        Policy p = valid();
        p.setIdleWindowMinutes(1);      // 6 samples fit in 1 minute at one sample / 10 s
        p.setMinSamplesRequired(7);
        String message = PolicyValidator.validate(p).orElseThrow();
        assertTrue(message.contains("minSamplesRequired"));

        p.setMinSamplesRequired(6);
        assertTrue(PolicyValidator.validate(p).isEmpty());
    }

    @Test
    void rejectsBadRecoveryStrikesAndGracePeriod() {
        Policy p = valid();
        p.setRecoveryStrikesRequired(0);
        assertTrue(PolicyValidator.validate(p).isPresent());

        p = valid();
        p.setGracePeriodSeconds(4);
        assertTrue(PolicyValidator.validate(p).isPresent());
        p.setGracePeriodSeconds(301);
        assertTrue(PolicyValidator.validate(p).isPresent());
    }

    @Test
    void rejectsNullBody() {
        assertEquals("Policy body is required.", PolicyValidator.validate(null).orElseThrow());
    }
}
