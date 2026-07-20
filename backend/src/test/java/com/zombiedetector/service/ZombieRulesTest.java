    package com.zombiedetector.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.zombiedetector.model.NodeMetric;

class ZombieRulesTest {

    private NodeMetric sample(double cpu, boolean traffic) {
        return new NodeMetric("test-node", cpu, traffic);
    }

    @Test
    void flagsNodeAsIdle_whenCpuLowAndNoTrafficAcrossAllSamples() {
        List<NodeMetric> samples = List.of(
                sample(2.0, false), sample(5.0, false), sample(1.0, false),
                sample(4.0, false), sample(3.0, false)
        );
        Optional<String> result = ZombieRules.checkIfZombie(samples, 15.0, 5, 2);
        assertTrue(result.isPresent(), "should flag a consistently quiet node");
    }

    @Test
    void doesNotFlag_whenNotEnoughSamplesYet() {
        List<NodeMetric> samples = List.of(sample(2.0, false), sample(1.0, false));
        Optional<String> result = ZombieRules.checkIfZombie(samples, 15.0, 5, 2);
        assertTrue(result.isEmpty(), "should not judge a node with insufficient history");
    }

    @Test
    void doesNotFlag_whenAnySampleHasTraffic() {
        List<NodeMetric> samples = List.of(
                sample(2.0, false), sample(1.0, false), sample(3.0, true), // one traffic spike
                sample(2.0, false), sample(1.0, false)
        );
        Optional<String> result = ZombieRules.checkIfZombie(samples, 15.0, 5, 2);
        assertTrue(result.isEmpty(), "a single traffic hit anywhere in the window should prevent flagging");
    }

    @Test
    void doesNotFlag_whenCpuSpikesEvenOnce() {
        List<NodeMetric> samples = List.of(
                sample(2.0, false), sample(1.0, false), sample(80.0, false), // one CPU spike
                sample(2.0, false), sample(1.0, false)
        );
        Optional<String> result = ZombieRules.checkIfZombie(samples, 15.0, 5, 2);
        assertTrue(result.isEmpty(), "a single high-CPU sample should prevent flagging, since it checks the max");
    }

    @Test
    void doesNotFlag_activeNode() {
        List<NodeMetric> samples = List.of(
                sample(45.0, true), sample(60.0, true), sample(30.0, true),
                sample(55.0, false), sample(40.0, true)
        );
        Optional<String> result = ZombieRules.checkIfZombie(samples, 15.0, 5, 2);
        assertTrue(result.isEmpty(), "a genuinely active node should never be flagged");
    }
}