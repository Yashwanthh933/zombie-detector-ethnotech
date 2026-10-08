package com.zombiedetector.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zombiedetector.model.Policy;

/**
 * Keeps a live list of connected SSE clients and pushes a "policy-updated" event to all of
 * them whenever an admin saves policy changes. In-memory, no external broker needed at this
 * scale (single instance) -- would need Redis pub/sub or similar across multiple instances.
 *
 * A periodic comment-line heartbeat keeps proxies (Render, Vercel rewrites, corporate
 * gateways) from closing idle streams, and lets us notice and drop dead connections without
 * waiting for the next policy change.
 */
@Service
public class PolicyNotificationService {

    private static final int MAX_CONNECTIONS = 500;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Bound memory: if somebody opens connections in a loop, evict the oldest.
        while (emitters.size() >= MAX_CONNECTIONS) {
            SseEmitter oldest = emitters.remove(0);
            try { oldest.complete(); } catch (Exception ignored) { /* already gone */ }
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        // Sent immediately so the browser's EventSource sees the stream as open.
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcastPolicyUpdated(Policy policy) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("policy-updated").data(policy));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }

    @Scheduled(fixedRate = 25000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }

    public int connectionCount() {
        return emitters.size();
    }
}
