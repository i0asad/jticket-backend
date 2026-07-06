package com.jticket.jticket_backend.services.impl;

import com.jticket.jticket_backend.entities.*;
import com.jticket.jticket_backend.repositories.TicketRepository;
import com.jticket.jticket_backend.repositories.UserRepository;
import com.jticket.jticket_backend.services.AzureBlobService;
import com.jticket.jticket_backend.services.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AzureBlobService azureBlobService;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             AzureBlobService azureBlobService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.azureBlobService = azureBlobService;
    }

    private void updateUserCredibility(UUID userId, int amount) {
        if (userId == null) return;
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getRole() != UserRole.ADMIN) {
                user.setCredibilityScore(user.getCredibilityScore() + amount);
                userRepository.save(user);
            }
        });
    }

    private void applyTicketRating(UUID clientId, Integer rating) {
        if (rating == null) return;
        if (rating == 5) updateUserCredibility(clientId, 5);
        else if (rating == 4) updateUserCredibility(clientId, 2);
        else if (rating <= 2) updateUserCredibility(clientId, -5);
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    @Override
    public Ticket createTicket(String title, String description,
                               LocalDate dueDate, MultipartFile file, UUID createdById) throws IOException {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = azureBlobService.uploadFile(file);
        }

        Ticket ticket = new Ticket(title, description, TicketPriority.LOW, createdById);
        ticket.setImageUrl(imageUrl);
        ticket.setDueDate(dueDate);
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyTickets(UUID userId, Pageable pageable) {
        return ticketRepository.findByCreatedById(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getAssignedTickets(UUID userId, Pageable pageable) {
        return ticketRepository.findByAssignedToId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> searchTickets(String keyword, Pageable pageable) {
        return ticketRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public Ticket approveTicket(UUID ticketId, UUID reviewerId, TicketPriority priority, Integer ticketRating) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new IllegalStateException("Only PENDING tickets can be approved");
        }
        ticket.setStatus(TicketStatus.APPROVED);
        ticket.setPriority(priority);
        ticket.setReviewedById(reviewerId);
        ticket.setTicketRating(ticketRating);
        applyTicketRating(ticket.getCreatedById(), ticketRating);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket rejectTicket(UUID ticketId, UUID reviewerId, Integer ticketRating) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new IllegalStateException("Only PENDING tickets can be rejected");
        }
        ticket.setStatus(TicketStatus.REJECTED);
        ticket.setRejectedById(reviewerId);
        ticket.setTicketRating(ticketRating);
        applyTicketRating(ticket.getCreatedById(), ticketRating);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket assignTicket(UUID ticketId, UUID assignedToId, UUID assignedById) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.APPROVED && ticket.getStatus() != TicketStatus.PASSED && ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException("Ticket must be APPROVED or PASSED or ASSIGNED to be assigned");
        }

        User assigner = userRepository.findById(assignedById)
                .orElseThrow(() -> new IllegalArgumentException("Assigner not found"));
        User worker = userRepository.findById(assignedToId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        if (assigner.getRole() == UserRole.MOD && worker.getRole() != UserRole.WORKER) {
            throw new IllegalArgumentException("Mods can only assign tickets to workers");
        }
        if (assigner.getRole() == UserRole.ADMIN && worker.getRole() != UserRole.WORKER && !assignedToId.equals(assignedById)) {
            throw new IllegalArgumentException("Admins can only assign tickets to workers or themselves");
        }

        if (ticket.getAssignedToId() != null && assigner.getRole() == UserRole.ADMIN) {
            if (ticket.getAssignedById() != null) {
                userRepository.findById(ticket.getAssignedById()).ifPresent(prevAssigner -> {
                    if (prevAssigner.getRole() == UserRole.MOD) {
                        updateUserCredibility(prevAssigner.getId(), -10);
                    }
                });
            }
        }

        ticket.setStatus(TicketStatus.ASSIGNED);
        ticket.setAssignedToId(assignedToId);
        ticket.setAssignedById(assignedById);
        ticket.setAssignedAt(Instant.now());
        return ticketRepository.save(ticket);
    }
    
    @Override
    public Ticket startWork(UUID ticketId, UUID workerId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException("Ticket must be ASSIGNED to start work");
        }
        if (!ticket.getAssignedToId().equals(workerId)) {
            throw new IllegalArgumentException("Only the assigned worker can start work");
        }
        
        int currentActive = ticketRepository.countByAssignedToIdAndStatusIn(workerId, List.of(TicketStatus.IN_PROGRESS));
        if (currentActive >= 3) {
            throw new IllegalStateException("You can only have up to 3 tickets in progress simultaneously");
        }
        
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket completeTicket(UUID ticketId, UUID userId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS && ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException("Only IN_PROGRESS or ASSIGNED tickets can be completed");
        }
        if (!ticket.getAssignedToId().equals(userId)) {
            throw new IllegalArgumentException("Only the assigned worker can complete this ticket");
        }

        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(Instant.now());
        
        userRepository.findById(userId).ifPresent(user -> {
            boolean onTime = ticket.getDueDate() == null || !LocalDate.now().isAfter(ticket.getDueDate());
            if (onTime) {
                user.setOnTimeCompletions(user.getOnTimeCompletions() + 1);
            } else {
                user.setLateCompletions(user.getLateCompletions() + 1);
            }
            userRepository.save(user);
        });

        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket cancelTicket(UUID ticketId, UUID reviewerId, Integer ticketRating) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User reviewer = userRepository.findById(reviewerId).orElseThrow();
        
        if (reviewer.getRole() == UserRole.MOD) {
            if (ticket.getStatus() != TicketStatus.PENDING && ticket.getStatus() != TicketStatus.APPROVED) {
                throw new IllegalStateException("Mods cannot cancel tickets that are already assigned or beyond");
            }
        }
        
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setRejectedById(reviewerId);
        ticket.setTicketRating(ticketRating);
        applyTicketRating(ticket.getCreatedById(), ticketRating);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket reviveTicket(UUID ticketId, UUID adminId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.REJECTED && ticket.getStatus() != TicketStatus.CANCELLED) {
            throw new IllegalStateException("Only REJECTED or CANCELLED tickets can be revived");
        }
        
        User admin = userRepository.findById(adminId).orElseThrow();
        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Only Admins can revive tickets");
        }
        
        if (ticket.getStatus() == TicketStatus.REJECTED && ticket.getRejectedById() != null) {
            userRepository.findById(ticket.getRejectedById()).ifPresent(rejecter -> {
                if (rejecter.getRole() == UserRole.MOD) {
                    updateUserCredibility(rejecter.getId(), -10);
                }
            });
        }
        
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setRejectedById(null);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket rateWorker(UUID ticketId, UUID clientId, Integer rating, String feedback) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED tickets can be rated");
        }
        if (!ticket.getCreatedById().equals(clientId)) {
            throw new IllegalArgumentException("Only the creator can rate this ticket");
        }
        if (ticket.getWorkerRating() != null) {
            throw new IllegalStateException("Ticket is already rated");
        }
        
        ticket.setWorkerRating(rating);
        ticket.setRatingFeedback(feedback);
        
        if (rating == 5) {
            updateUserCredibility(ticket.getAssignedToId(), 10);
            updateUserCredibility(ticket.getAssignedById(), 5);
            updateUserCredibility(clientId, 5);
        } else if (rating == 4) {
            updateUserCredibility(ticket.getAssignedToId(), 5);
            updateUserCredibility(ticket.getAssignedById(), 2);
            updateUserCredibility(clientId, 2);
        } else if (rating <= 2) {
            updateUserCredibility(ticket.getAssignedToId(), -10);
            updateUserCredibility(ticket.getAssignedById(), -5);
        }
        
        return ticketRepository.save(ticket);
    }

    @Override
    public void deleteTicket(UUID ticketId) {
        ticketRepository.deleteById(ticketId);
    }
}
