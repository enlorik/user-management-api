package com.empress.usermanagementapi.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.entity.Role;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Check if an email is already in use.
     */
    public boolean emailExists(String email) {
        return userRepo.existsByEmail(email);
    }

    /**
     * Check if a username is already in use.
     */
    public boolean usernameExists(String username) {
        return userRepo.existsByUsername(username);
    }

    /**
     * Create a new user (caller is responsible for checking duplicates first).
     */
    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        return userRepo.save(user);
    }

    // ← below this line, the original file’s other methods follow, unchanged

    public List<User> findAll() {
        return userRepo.findAll();
    }

    /**
     * Find all users sorted by a specific field and direction.
     */
    public List<User> findAll(Sort sort) {
        return userRepo.findAll(sort);
    }

    public Optional<User> findById(Long id) {
        return userRepo.findById(id);
    }

    /**
     * Find a user by username.
     */
    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    /**
     * Find a user by username and email combination.
     */
    public Optional<User> findByUsernameAndEmail(String username, String email) {
        return userRepo.findByUsernameAndEmail(username, email);
    }

    /**
     * Check if a user exists by ID.
     */
    public boolean existsById(Long id) {
        return userRepo.existsById(id);
    }

    public User update(User user) {
        return userRepo.save(user);
    }

    /**
     * Update user with password encoding if password is provided.
     * This method should be used when updating a user with a raw (unencoded) password.
     */
    public User updateWithPassword(User user, String rawPassword) {
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        return userRepo.save(user);
    }

    public void deleteById(Long id) {
        userRepo.deleteById(id);
    }
}
