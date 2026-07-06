package com.jticket.jticket_backend.repositories;

import com.jticket.jticket_backend.entities.User;
import com.jticket.jticket_backend.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(UserRole role);
    java.util.List<User> findByRoleIn(java.util.List<UserRole> roles);
}
