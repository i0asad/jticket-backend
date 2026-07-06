package com.jticket.jticket_backend.services;

import com.jticket.jticket_backend.entities.PassRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface PassRequestService {
    PassRequest createPassRequest(UUID ticketId, UUID workerId, String reason);
    Page<PassRequest> getPendingPassRequests(Pageable pageable);
    PassRequest approvePassRequest(UUID passRequestId, UUID reviewerId);
    PassRequest denyPassRequest(UUID passRequestId, UUID reviewerId);
}
