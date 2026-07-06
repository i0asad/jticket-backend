package com.jticket.jticket_backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @NotNull(message = "Created by ID is required")
    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "reviewed_by_id")
    private UUID reviewedById;

    @Column(name = "assigned_by_id")
    private UUID assignedById;

    @Column(name = "rejected_by_id")
    private UUID rejectedById;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(name = "ticket_rating")
    private Integer ticketRating;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(name = "worker_rating")
    private Integer workerRating;

    @Size(max = 1000, message = "Rating feedback cannot exceed 1000 characters")
    @Column(name = "rating_feedback", length = 1000)
    private String ratingFeedback;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Ticket(String title, String description, TicketPriority priority, UUID createdById) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdById = createdById;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}

