package com.jticket.jticket_backend.controllers;

import com.jticket.jticket_backend.entities.PassRequest;
import com.jticket.jticket_backend.services.PassRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pass-requests")
public class PassRequestController {

    private final PassRequestService passRequestService;

    public PassRequestController(PassRequestService passRequestService) {
        this.passRequestService = passRequestService;
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<PassRequest>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(passRequestService.getPendingPassRequests(pageable));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID reviewerId = (UUID) authentication.getDetails();
            PassRequest result = passRequestService.approvePassRequest(id, reviewerId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID reviewerId = (UUID) authentication.getDetails();
            PassRequest result = passRequestService.denyPassRequest(id, reviewerId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
