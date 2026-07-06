package com.jticket.jticket_backend.controllers;

import com.jticket.jticket_backend.dto.TicketResponse;
import com.jticket.jticket_backend.entities.Ticket;
import com.jticket.jticket_backend.entities.TicketPriority;
import com.jticket.jticket_backend.entities.TicketStatus;
import com.jticket.jticket_backend.entities.UserRole;
import com.jticket.jticket_backend.services.AzureBlobService;
import com.jticket.jticket_backend.services.PassRequestService;
import com.jticket.jticket_backend.services.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final PassRequestService passRequestService;
    private final AzureBlobService azureBlobService;

    public TicketController(TicketService ticketService, PassRequestService passRequestService, AzureBlobService azureBlobService) {
        this.ticketService = ticketService;
        this.passRequestService = passRequestService;
        this.azureBlobService = azureBlobService;
    }

    private UserRole getCallerRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.CLIENT);
    }

    private TicketResponse toResponse(Ticket ticket, Authentication authentication) {
        if (ticket == null) return null;
        UUID callerId = (UUID) authentication.getDetails();
        UserRole callerRole = getCallerRole(authentication);
        String presignedUrl = azureBlobService.generatePresignedUrl(ticket.getImageUrl());
        return TicketResponse.fromTicket(ticket, presignedUrl, callerId, callerRole);
    }

    private Page<TicketResponse> toResponsePage(Page<Ticket> page, Authentication authentication) {
        if (page == null) return null;
        UUID callerId = (UUID) authentication.getDetails();
        UserRole callerRole = getCallerRole(authentication);
        return page.map(ticket -> {
            String presignedUrl = azureBlobService.generatePresignedUrl(ticket.getImageUrl());
            return TicketResponse.fromTicket(ticket, presignedUrl, callerId, callerRole);
        });
    }

    @PostMapping
    public ResponseEntity<?> createTicket(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "dueDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        try {
            UUID userId = (UUID) authentication.getDetails();
            UserRole callerRole = getCallerRole(authentication);
            if (callerRole == UserRole.WORKER || callerRole == UserRole.MOD) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Workers and moderators are not allowed to create ticket requests"));
            }
            Ticket ticket = ticketService.createTicket(title, description, dueDate, file, userId);
            return new ResponseEntity<>(toResponse(ticket, authentication), HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file"));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyTickets(Authentication authentication, Pageable pageable) {
        UUID userId = (UUID) authentication.getDetails();
        Page<Ticket> page = ticketService.getMyTickets(userId, pageable);
        return ResponseEntity.ok(toResponsePage(page, authentication));
    }

    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedTickets(Authentication authentication, Pageable pageable) {
        UUID userId = (UUID) authentication.getDetails();
        Page<Ticket> page = ticketService.getAssignedTickets(userId, pageable);
        return ResponseEntity.ok(toResponsePage(page, authentication));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTickets(Pageable pageable, Authentication authentication) {
        Page<Ticket> page = ticketService.getAllTickets(pageable);
        return ResponseEntity.ok(toResponsePage(page, authentication));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getTicketsByStatus(@PathVariable String status, Pageable pageable, Authentication authentication) {
        try {
            TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
            Page<Ticket> page = ticketService.getTicketsByStatus(ticketStatus, pageable);
            return ResponseEntity.ok(toResponsePage(page, authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTickets(@RequestParam("keyword") String keyword, Pageable pageable, Authentication authentication) {
        Page<Ticket> page = ticketService.searchTickets(keyword, pageable);
        return ResponseEntity.ok(toResponsePage(page, authentication));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveTicket(@PathVariable UUID id, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            UUID reviewerId = (UUID) authentication.getDetails();
            TicketPriority priority = TicketPriority.valueOf((String) request.get("priority"));
            Integer rating = request.containsKey("ticketRating") ? (Integer) request.get("ticketRating") : null;
            Ticket ticket = ticketService.approveTicket(id, reviewerId, priority, rating);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectTicket(@PathVariable UUID id, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            UUID reviewerId = (UUID) authentication.getDetails();
            Integer rating = request.containsKey("ticketRating") ? (Integer) request.get("ticketRating") : null;
            Ticket ticket = ticketService.rejectTicket(id, reviewerId, rating);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignTicket(@PathVariable UUID id, @RequestParam UUID assignedToId, Authentication authentication) {
        try {
            UUID assignedById = (UUID) authentication.getDetails();
            Ticket ticket = ticketService.assignTicket(id, assignedToId, assignedById);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/start")
    public ResponseEntity<?> startWork(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID workerId = (UUID) authentication.getDetails();
            Ticket ticket = ticketService.startWork(id, workerId);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeTicket(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID userId = (UUID) authentication.getDetails();
            Ticket ticket = ticketService.completeTicket(id, userId);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTicket(@PathVariable UUID id, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            UUID reviewerId = (UUID) authentication.getDetails();
            Integer rating = request.containsKey("ticketRating") ? (Integer) request.get("ticketRating") : null;
            Ticket ticket = ticketService.cancelTicket(id, reviewerId, rating);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/revive")
    public ResponseEntity<?> reviveTicket(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID adminId = (UUID) authentication.getDetails();
            Ticket ticket = ticketService.reviveTicket(id, adminId);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/rate")
    public ResponseEntity<?> rateWorker(@PathVariable UUID id, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            UUID clientId = (UUID) authentication.getDetails();
            Integer rating = (Integer) request.get("workerRating");
            String feedback = (String) request.get("ratingFeedback");
            Ticket ticket = ticketService.rateWorker(id, clientId, rating, feedback);
            return ResponseEntity.ok(toResponse(ticket, authentication));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTicket(@PathVariable UUID id) {
        try {
            ticketService.deleteTicket(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/pass-request")
    public ResponseEntity<?> requestPass(@PathVariable UUID id, @RequestBody Map<String, String> request, Authentication authentication) {
        try {
            UUID workerId = (UUID) authentication.getDetails();
            String reason = request.get("reason");
            return ResponseEntity.ok(passRequestService.createPassRequest(id, workerId, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

