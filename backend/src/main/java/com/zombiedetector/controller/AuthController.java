package com.zombiedetector.controller;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zombiedetector.dto.AuthDtos.AuthResponse;
import com.zombiedetector.dto.AuthDtos.LoginRequest;
import com.zombiedetector.dto.AuthDtos.MeResponse;
import com.zombiedetector.dto.AuthDtos.RegisterRequest;
import com.zombiedetector.model.Role;
import com.zombiedetector.model.User;
import com.zombiedetector.repository.UserRepository;
import com.zombiedetector.security.AuthRateLimiter;
import com.zombiedetector.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt only uses the first 72 bytes

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthRateLimiter rateLimiter;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService,
                           AuthRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest http) {
        if (!rateLimiter.tryAcquire("auth:" + clientIp(http))) {
            return tooManyRequests();
        }

        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();

        if (email == null || !EMAIL.matcher(email).matches() || email.length() > 254) {
            return ResponseEntity.badRequest().body("Please enter a valid email address.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return ResponseEntity.badRequest().body("Password must be at most " + MAX_PASSWORD_BYTES + " bytes.");
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body("An account with that email already exists.");
        }

        User user = new User(email, passwordEncoder.encode(password), Role.USER);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Two simultaneous sign-ups for the same email: the unique constraint is the real guard.
            return ResponseEntity.status(409).body("An account with that email already exists.");
        }

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        if (!rateLimiter.tryAcquire("auth:" + clientIp(http))) {
            return tooManyRequests();
        }

        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();
        if (email == null || password == null) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name()));
    }

    /** Lets the frontend restore a session after a page reload and learn the caller's role. */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }
        return ResponseEntity.ok(new MeResponse(user.getEmail(), user.getRole().name()));
    }

    private static String normalizeEmail(String email) {
        if (email == null) return null;
        String trimmed = email.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Render (and most hosts) put the real client IP first in X-Forwarded-For. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static ResponseEntity<String> tooManyRequests() {
        return ResponseEntity.status(429).body("Too many attempts. Please wait a minute and try again.");
    }
}
