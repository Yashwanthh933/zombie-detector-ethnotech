package com.zombiedetector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.Role;
import com.zombiedetector.model.User;
import com.zombiedetector.repository.UserRepository;

/**
 * Creates exactly one admin account on first boot, if none exists yet, so there's a way
 * to log into the admin panel without a manual DB insert. Change ADMIN_PASSWORD before
 * any real deployment -- the default below is a dev-only placeholder.
 */
@Component
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@zombiedetector.local}")
    private String adminEmail;

    @Value("${app.admin.password:ChangeMe123!}")
    private String adminPassword;

    public AdminUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (System.getenv("RENDER") != null && "ChangeMe123!".equals(adminPassword)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is still the committed default. Set the ADMIN_PASSWORD environment variable.");
        }

        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) return;

        User admin = new User(email, passwordEncoder.encode(adminPassword), Role.ADMIN);
        userRepository.save(admin);
        System.out.println("Seeded default admin account: " + email
                + " (change app.admin.password before any real deployment)");
    }
}