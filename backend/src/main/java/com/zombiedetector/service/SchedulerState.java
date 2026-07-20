package com.zombiedetector.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component
public class SchedulerState {
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public boolean isPaused() { return paused.get(); }
    public void pause() { paused.set(true); }
    public void resume() { paused.set(false); }
}