package com.jticket.jticket_backend.repositories;

import com.jticket.jticket_backend.entities.Ticket;
import com.jticket.jticket_backend.entities.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Page<Ticket> findByCreatedById(UUID userId, Pageable pageable);
    Page<Ticket> findByAssignedToId(UUID userId, Pageable pageable);
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
    
    int countByAssignedToIdAndStatusIn(UUID workerId, List<TicketStatus> statuses);
}
