package com.jticket.jticket_backend.services.impl;

import com.jticket.jticket_backend.entities.User;
import com.jticket.jticket_backend.entities.UserRole;
import com.jticket.jticket_backend.repositories.UserRepository;
import com.jticket.jticket_backend.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public User changeRole(UUID userId, UserRole newRole) {
        User user = getUserById(userId);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Override
    public User banUser(UUID userId) {
        User user = getUserById(userId);
        user.setBanned(true);
        return userRepository.save(user);
    }

    @Override
    public User unbanUser(UUID userId) {
        User user = getUserById(userId);
        user.setBanned(false);
        return userRepository.save(user);
    }

    @Override
    public void forceResetPassword(UUID userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchWorkers(String query, UUID requestorId) {
        User requestor = getUserById(requestorId);
        
        List<User> workers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.WORKER || (requestor.getRole() == UserRole.ADMIN && u.getId().equals(requestorId)))
                .filter(u -> query == null || query.isBlank() ||
                        u.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                        u.getEmail().toLowerCase().contains(query.toLowerCase()) ||
                        u.getId().toString().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        return workers;
    }
}
