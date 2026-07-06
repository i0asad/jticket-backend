package com.jticket.jticket_backend.services;

import com.jticket.jticket_backend.entities.User;
import com.jticket.jticket_backend.entities.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {
    Page<User> getAllUsers(Pageable pageable);
    User getUserById(UUID id);
    User changeRole(UUID userId, UserRole newRole);
    User banUser(UUID userId);
    User unbanUser(UUID userId);
    void forceResetPassword(UUID userId, String newPassword);
    List<User> searchWorkers(String query, UUID requestorId);
}
