package com.jticket.jticket_backend.services.impl;

import com.jticket.jticket_backend.entities.*;
import com.jticket.jticket_backend.repositories.PassRequestRepository;
import com.jticket.jticket_backend.repositories.TicketRepository;
import com.jticket.jticket_backend.repositories.UserRepository;
import com.jticket.jticket_backend.services.PassRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PassRequestServiceImpl implements PassRequestService {

    private final PassRequestRepository passRequestRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public PassRequestServiceImpl(PassRequestRepository passRequestRepository,
                                  TicketRepository ticketRepository,
                                  UserRepository userRepository) {
        this.passRequestRepository = passRequestRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PassRequest createPassRequest(UUID ticketId, UUID workerId, String reason) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.ASSIGNED && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only request pass on ASSIGNED or IN_PROGRESS tickets");
        }
        if (!ticket.getAssignedToId().equals(workerId)) {
            throw new IllegalArgumentException("Only the assigned worker can request a pass");
        }

        List<PassRequest> existing = passRequestRepository
                .findByTicketIdAndStatus(ticketId, PassRequestStatus.PENDING_PASS);
        if (!existing.isEmpty()) {
            throw new IllegalStateException("There is already a pending pass request for this ticket");
        }

        PassRequest passRequest = new PassRequest(ticketId, workerId, reason);
        return passRequestRepository.save(passRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PassRequest> getPendingPassRequests(Pageable pageable) {
        return passRequestRepository.findByStatus(PassRequestStatus.PENDING_PASS, pageable);
    }

    @Override
    public PassRequest approvePassRequest(UUID passRequestId, UUID reviewerId) {
        PassRequest passRequest = findOrThrow(passRequestId);

        if (passRequest.getStatus() != PassRequestStatus.PENDING_PASS) {
            throw new IllegalStateException("Pass request is not pending");
        }

        passRequest.setStatus(PassRequestStatus.PASS_APPROVED);
        passRequest.setReviewedById(reviewerId);
        passRequest.setReviewedAt(Instant.now());

        Ticket ticket = ticketRepository.findById(passRequest.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        
        // Penalize the assigning mod
        if (ticket.getAssignedById() != null) {
            userRepository.findById(ticket.getAssignedById()).ifPresent(assigner -> {
                if (assigner.getRole() == UserRole.MOD) {
                    assigner.setCredibilityScore(assigner.getCredibilityScore() - 10);
                    userRepository.save(assigner);
                }
            });
        }
        
        ticket.setStatus(TicketStatus.PASSED);
        ticket.setAssignedToId(null);
        ticket.setAssignedById(null);
        ticket.setAssignedAt(null);
        ticketRepository.save(ticket);

        User worker = userRepository.findById(passRequest.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        worker.setPassedTickets(worker.getPassedTickets() + 1);
        if (worker.getRole() != UserRole.ADMIN) {
            worker.setCredibilityScore(worker.getCredibilityScore() - 5);
        }
        userRepository.save(worker);

        return passRequestRepository.save(passRequest);
    }

    @Override
    public PassRequest denyPassRequest(UUID passRequestId, UUID reviewerId) {
        PassRequest passRequest = findOrThrow(passRequestId);

        if (passRequest.getStatus() != PassRequestStatus.PENDING_PASS) {
            throw new IllegalStateException("Pass request is not pending");
        }

        passRequest.setStatus(PassRequestStatus.PASS_DENIED);
        passRequest.setReviewedById(reviewerId);
        passRequest.setReviewedAt(Instant.now());

        User worker = userRepository.findById(passRequest.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        if (worker.getRole() != UserRole.ADMIN) {
            worker.setCredibilityScore(worker.getCredibilityScore() - 15);
        }
        userRepository.save(worker);

        return passRequestRepository.save(passRequest);
    }

    private PassRequest findOrThrow(UUID id) {
        return passRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass request not found"));
    }
}
