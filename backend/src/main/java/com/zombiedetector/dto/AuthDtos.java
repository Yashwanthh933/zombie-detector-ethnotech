package com.zombiedetector.dto;

/** Grouped together since these are small and only used by AuthController. */
public class AuthDtos {

    public record RegisterRequest(String email, String password) {}

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(String token, String email, String role) {}

    public record MeResponse(String email, String role) {}

    private AuthDtos() {}
}