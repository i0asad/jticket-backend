package com.jticket.jticket_backend.services;

import java.util.Map;

public interface AuthService {
    Map<String, Object> register(String username, String email, String password);
    Map<String, Object> login(String username, String password);
    void changePassword(String username, String oldPassword, String newPassword);
}
