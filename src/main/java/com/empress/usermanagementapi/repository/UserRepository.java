package com.empress.usermanagementapi.repository;

import com.empress.usermanagementapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameAndEmail(String username, String email);
    User findByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
