package com.jticket.jticket_backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pass_requests")
@Getter
@Setter
@NoArgsConstructor
public class PassRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Ticket ID is required")
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @NotNull(message = "Worker ID is required")
    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassRequestStatus status = PassRequestStatus.PENDING_PASS;

    @Column(name = "reviewed_by_id")
    private UUID reviewedById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public PassRequest(UUID ticketId, UUID workerId, String reason) {
        this.ticketId = ticketId;
        this.workerId = workerId;
        this.reason = reason;
    }
}

