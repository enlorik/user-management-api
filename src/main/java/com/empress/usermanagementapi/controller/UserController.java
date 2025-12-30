package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.CreateUserRequest;
import com.empress.usermanagementapi.dto.UserResponse;
import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.UserService;
import jakarta.validation.Valid;
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
import java.util.stream.Collectors;

/**
 * REST controller for managing user resources.
 * 
 * This controller provides CRUD operations for users and uses Bean Validation
 * to ensure data integrity. Validation errors are automatically handled by the
 * GlobalExceptionHandler and returned as structured JSON responses.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // return all users sorted by id (ascending)
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new user with validation.
     * 
     * This endpoint validates the user creation request using Bean Validation.
     * If validation fails, the GlobalExceptionHandler automatically catches the
     * exception and returns a structured error response with details about which
     * fields failed validation.
     * 
     * After validation passes, it checks for duplicate username/email before
     * creating the user.
     * 
     * @param request The user creation request with validation annotations
     * @return ResponseEntity with the created user or error details
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        // duplicate-username guard
        if (userService.usernameExists(request.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A user with this username already exists"));
        }
        // duplicate-email guard
        if (userService.emailExists(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A user with this email already exists"));
        }

        // Create user entity from validated request
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);
        
        User saved = userService.create(user);
        UserResponse response = UserResponse.fromEntity(saved);
        return ResponseEntity
                .created(URI.create("/users/" + saved.getId()))
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                           @RequestBody User user) {
        Optional<User> opt = userService.findById(id);
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
        User updated = userService.update(existing);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ——— Self-service endpoint ———
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String,String> body
    ) {
        User me = userService.findByUsername(principal.getUsername());
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
                    && userService.emailExists(newEmail)) {
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

        User saved = userService.update(me);
        return ResponseEntity.ok(UserResponse.fromEntity(saved));
    }
}
