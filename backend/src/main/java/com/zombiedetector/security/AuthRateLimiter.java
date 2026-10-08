package com.zombiedetector.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sliding-window limiter for the public auth endpoints (login/register), keyed by caller IP.
 * In-memory and per-instance, which is fine for a single backend; a multi-instance deployment
 * would move this to a shared store.
 */
@Component
public class AuthRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimiter(@Value("${app.auth.rate-limit.max-attempts:10}") int maxAttempts,
                           @Value("${app.auth.rate-limit.window-seconds:60}") int windowSeconds) {
        this(maxAttempts, windowSeconds * 1000L, System::currentTimeMillis);
    }

    // Package-private: lets tests control time.
    AuthRateLimiter(int maxAttempts, long windowMillis, LongSupplier clock) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** Records an attempt for the key; returns false if the key has exceeded its allowance. */
    public boolean tryAcquire(String key) {
        long now = clock.getAsLong();
        if (hits.size() > MAX_TRACKED_KEYS) hits.clear(); // memory guard against key-spraying

        Deque<Long> attempts = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && now - attempts.peekFirst() >= windowMillis) {
                attempts.pollFirst();
            }
            if (attempts.size() >= maxAttempts) return false;
            attempts.addLast(now);
            return true;
        }
    }

    @Scheduled(fixedDelay = 300_000)
    public void purgeStale() {
        long now = clock.getAsLong();
        hits.entrySet().removeIf(entry -> {
            Deque<Long> attempts = entry.getValue();
            synchronized (attempts) {
                return attempts.isEmpty() || now - attempts.peekLast() >= windowMillis;
            }
        });
    }
}
