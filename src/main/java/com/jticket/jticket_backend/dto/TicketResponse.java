package com.jticket.jticket_backend.dto;

import com.jticket.jticket_backend.entities.Ticket;
import com.jticket.jticket_backend.entities.TicketPriority;
import com.jticket.jticket_backend.entities.TicketStatus;
import com.jticket.jticket_backend.entities.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class TicketResponse {
    private UUID id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private UUID createdById;
    private UUID assignedToId;
    private UUID reviewedById;
    private UUID assignedById;
    private UUID rejectedById;
    private Integer ticketRating;
    private Integer workerRating;
    private String ratingFeedback;
    private String imageUrl;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant assignedAt;
    private Instant completedAt;

    public TicketResponse() {}

    public static TicketResponse fromTicket(Ticket ticket, String presignedUrl, UUID callerId, UserRole callerRole) {
        TicketResponse resp = new TicketResponse();
        resp.id = ticket.getId();
        resp.title = ticket.getTitle();
        resp.description = ticket.getDescription();
        resp.status = ticket.getStatus();
        resp.priority = ticket.getPriority();
        resp.createdById = ticket.getCreatedById();
        resp.assignedToId = ticket.getAssignedToId();
        resp.reviewedById = ticket.getReviewedById();
        resp.assignedById = ticket.getAssignedById();
        resp.rejectedById = ticket.getRejectedById();
        resp.imageUrl = presignedUrl;
        resp.dueDate = ticket.getDueDate();
        resp.createdAt = ticket.getCreatedAt();
        resp.updatedAt = ticket.getUpdatedAt();
        resp.assignedAt = ticket.getAssignedAt();
        resp.completedAt = ticket.getCompletedAt();

        // Hide request rating from client
        if (callerRole != UserRole.ADMIN && callerId != null && callerId.equals(ticket.getCreatedById())) {
            resp.ticketRating = null;
        } else {
            resp.ticketRating = ticket.getTicketRating();
        }

        // Hide work rating from worker/mod
        if (callerRole == UserRole.WORKER || callerRole == UserRole.MOD) {
            resp.workerRating = null;
            resp.ratingFeedback = null;
        } else {
            resp.workerRating = ticket.getWorkerRating();
            resp.ratingFeedback = ticket.getRatingFeedback();
        }

        return resp;
    }
}

