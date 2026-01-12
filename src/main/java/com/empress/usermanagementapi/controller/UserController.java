package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.CreateUserRequest;
import com.empress.usermanagementapi.dto.UserResponse;
import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.UserService;
import com.empress.usermanagementapi.util.LoggingUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // return all users sorted by id (ascending)
    @GetMapping
    public List<UserResponse> getAllUsers() {
        log.debug("Retrieving all users");
        List<UserResponse> users = userService.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        log.debug("Retrieved {} users", users.size());
        return users;
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
        LoggingUtil.setActionType("USER_CREATE");
        log.info("Received request to create user - username: {}, email: {}", 
                request.getUsername(), 
                LoggingUtil.maskEmail(request.getEmail()));
        
        // duplicate-username guard
        if (userService.usernameExists(request.getUsername())) {
            log.warn("User creation failed - username already exists: {}", request.getUsername());
            LoggingUtil.clearActionType();
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A user with this username already exists"));
        }
        // duplicate-email guard
        if (userService.emailExists(request.getEmail())) {
            log.warn("User creation failed - email already exists: {}", 
                    LoggingUtil.maskEmail(request.getEmail()));
            LoggingUtil.clearActionType();
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
        
        LoggingUtil.setUserId(saved.getId());
        log.info("User created successfully - userId: {}, username: {}", saved.getId(), saved.getUsername());
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();
        
        return ResponseEntity
                .created(URI.create("/users/" + saved.getId()))
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                           @RequestBody User user) {
        LoggingUtil.setActionType("USER_UPDATE");
        LoggingUtil.setUserId(id);
        log.info("Received request to update user - userId: {}", id);
        
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) {
            log.warn("User update failed - user not found: {}", id);
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return ResponseEntity.notFound().build();
        }

        User existing = opt.get();
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setRole(user.getRole());
        
        User updated = userService.updateWithPassword(existing, user.getPassword());
        log.info("User updated successfully - userId: {}", id);
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        LoggingUtil.setActionType("USER_DELETE");
        LoggingUtil.setUserId(id);
        log.info("Received request to delete user - userId: {}", id);
        
        if (!userService.existsById(id)) {
            log.warn("User deletion failed - user not found: {}", id);
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        log.info("User deleted successfully - userId: {}", id);
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();
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
            User saved = userService.updateWithPassword(me, rawPassword);
            return ResponseEntity.ok(UserResponse.fromEntity(saved));
        }

        User saved = userService.update(me);
        return ResponseEntity.ok(UserResponse.fromEntity(saved));
    }
}
