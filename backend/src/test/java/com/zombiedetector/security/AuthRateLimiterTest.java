package com.zombiedetector.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    @Test
    void blocksAfterTheAllowanceIsUsedUp() {
        AtomicLong now = new AtomicLong(0);
        AuthRateLimiter limiter = new AuthRateLimiter(3, 60_000, now::get);

        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertFalse(limiter.tryAcquire("1.2.3.4"), "4th attempt inside the window must be rejected");
    }

    @Test
    void keysAreIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 60_000, () -> 0L);

        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"), "a different caller is unaffected");
    }

    @Test
    void allowanceRecoversWhenTheWindowSlides() {
        AtomicLong now = new AtomicLong(0);
        AuthRateLimiter limiter = new AuthRateLimiter(2, 60_000, now::get);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));

        now.set(60_001);
        assertTrue(limiter.tryAcquire("a"), "old attempts have aged out of the window");
    }

    @Test
    void purgeDropsStaleEntriesWithoutBreakingAnything() {
        AtomicLong now = new AtomicLong(0);
        AuthRateLimiter limiter = new AuthRateLimiter(1, 1_000, now::get);

        assertTrue(limiter.tryAcquire("a"));
        now.set(5_000);
        limiter.purgeStale();
        assertTrue(limiter.tryAcquire("a"));
    }
}
