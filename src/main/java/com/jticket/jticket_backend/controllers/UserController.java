package com.jticket.jticket_backend.controllers;

import com.jticket.jticket_backend.entities.User;
import com.jticket.jticket_backend.entities.UserRole;
import com.jticket.jticket_backend.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/search-workers")
    public ResponseEntity<java.util.List<User>> searchWorkers(
            @RequestParam(value = "query", required = false) String query,
            Authentication authentication) {
        UUID requestorId = (UUID) authentication.getDetails();
        return ResponseEntity.ok(userService.searchWorkers(query, requestorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable UUID id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/credibility")
    public ResponseEntity<?> getCredibility(@PathVariable UUID id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().name(),
                    "credibilityScore", user.getRole() == UserRole.ADMIN ? 100 : user.getCredibilityScore(),
                    "completedCount", user.getOnTimeCompletions(),
                    "passedCount", user.getPassedTickets(),
                    "overdueCount", user.getLateCompletions()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        try {
            UserRole newRole = UserRole.valueOf(request.get("role"));
            User user = userService.changeRole(id, newRole);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/ban")
    public ResponseEntity<User> banUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.banUser(id));
    }

    @PutMapping("/{id}/unban")
    public ResponseEntity<User> unbanUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.unbanUser(id));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        userService.forceResetPassword(id, request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
