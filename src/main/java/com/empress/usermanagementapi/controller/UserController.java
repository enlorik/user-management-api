package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepo,
                          PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // return all users sorted by id (ascending)
    @GetMapping
    public List<User> getAllUsers() {
        return userRepo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        // duplicate-username guard
        if (userRepo.existsByUsername(user.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A user with this username already exists"));
        }
        // duplicate-email guard
        if (userRepo.existsByEmail(user.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A user with this email already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepo.save(user);
        return ResponseEntity
                .created(URI.create("/users/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @RequestBody User user) {
        Optional<User> opt = userRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existing = opt.get();
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        existing.setRole(user.getRole());
        return ResponseEntity.ok(userRepo.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ——— Self-service endpoint ———
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String,String> body
    ) {
        User me = userRepo.findByUsername(principal.getUsername());
        if (me == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // handle email change
        if (body.containsKey("email")) {
            String newEmail = body.get("email");
            if (newEmail != null) {
                newEmail = newEmail.trim();
            }

            if (newEmail == null || newEmail.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "Email cannot be empty."));
            }

            // if user is actually changing to a different email, enforce uniqueness
            if (!newEmail.equalsIgnoreCase(me.getEmail())
                    && userRepo.existsByEmail(newEmail)) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email is already in use."));
            }

            me.setEmail(newEmail);
        }

        // handle password change
        if (body.containsKey("password")) {
            String rawPassword = body.get("password");
            if (rawPassword != null && !rawPassword.isEmpty()) {
                me.setPassword(passwordEncoder.encode(rawPassword));
            }
        }

        User saved = userRepo.save(me);
        return ResponseEntity.ok(saved);
    }
}
