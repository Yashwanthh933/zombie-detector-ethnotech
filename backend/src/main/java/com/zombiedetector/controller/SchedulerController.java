package com.zombiedetector.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.service.SchedulerState;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerState schedulerState;

    public SchedulerController(SchedulerState schedulerState) {
        this.schedulerState = schedulerState;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("paused", schedulerState.isPaused());
    }

    @PostMapping("/pause")
    public Map<String, Object> pause() {
        schedulerState.pause();
        return Map.of("paused", true);
    }

    @PostMapping("/resume")
    public Map<String, Object> resume() {
        schedulerState.resume();
        return Map.of("paused", false);
    }
}