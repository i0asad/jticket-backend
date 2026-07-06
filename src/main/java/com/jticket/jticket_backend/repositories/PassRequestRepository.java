package com.jticket.jticket_backend.repositories;

import com.jticket.jticket_backend.entities.PassRequest;
import com.jticket.jticket_backend.entities.PassRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PassRequestRepository extends JpaRepository<PassRequest, UUID> {
    Page<PassRequest> findByStatus(PassRequestStatus status, Pageable pageable);
    List<PassRequest> findByTicketIdAndStatus(UUID ticketId, PassRequestStatus status);
    List<PassRequest> findByWorkerId(UUID workerId);
}
