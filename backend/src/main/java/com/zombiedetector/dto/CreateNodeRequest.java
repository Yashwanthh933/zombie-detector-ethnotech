package com.zombiedetector.dto;

/**
 * What a user submits when manually registering a resource to monitor. No real cloud
 * connection exists yet, so this is metadata only -- matches ManagedNode's existing fields.
 */
public record CreateNodeRequest(String instanceType, double hourlyRate, String environment) {
}