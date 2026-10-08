package com.zombiedetector.service;

import java.util.Optional;

import com.zombiedetector.model.Policy;

/**
 * Server-side bounds for policy values. The UI sliders enforce the same limits, but the API
 * must not trust the client: a window of 0 or a min-sample count the window can never reach
 * would silently switch detection off (or on for everything).
 */
public final class PolicyValidator {

    /** TrafficSimulator writes one sample per node every 10 seconds. */
    public static final int SAMPLE_INTERVAL_SECONDS = 10;

    private PolicyValidator() {}

    /** Returns an error message, or empty if the policy is acceptable. */
    public static Optional<String> validate(Policy p) {
        if (p == null) return Optional.of("Policy body is required.");

        double cpu = p.getCpuThreshold();
        if (Double.isNaN(cpu) || cpu < 0 || cpu > 100) {
            return Optional.of("cpuThreshold must be between 0 and 100.");
        }
        if (p.getIdleWindowMinutes() < 1 || p.getIdleWindowMinutes() > 120) {
            return Optional.of("idleWindowMinutes must be between 1 and 120.");
        }
        if (p.getMinSamplesRequired() < 1 || p.getMinSamplesRequired() > 20) {
            return Optional.of("minSamplesRequired must be between 1 and 20.");
        }
        int samplesInWindow = p.getIdleWindowMinutes() * 60 / SAMPLE_INTERVAL_SECONDS;
        if (p.getMinSamplesRequired() > samplesInWindow) {
            return Optional.of("minSamplesRequired (" + p.getMinSamplesRequired() + ") cannot exceed the "
                    + samplesInWindow + " samples that fit in a " + p.getIdleWindowMinutes()
                    + "-minute window, or no node could ever be flagged.");
        }
        if (p.getRecoveryStrikesRequired() < 1 || p.getRecoveryStrikesRequired() > 10) {
            return Optional.of("recoveryStrikesRequired must be between 1 and 10.");
        }
        if (p.getGracePeriodSeconds() < 5 || p.getGracePeriodSeconds() > 300) {
            return Optional.of("gracePeriodSeconds must be between 5 and 300.");
        }
        return Optional.empty();
    }
}
