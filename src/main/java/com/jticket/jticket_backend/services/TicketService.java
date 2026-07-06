package com.jticket.jticket_backend.services;

import com.jticket.jticket_backend.entities.Ticket;
import com.jticket.jticket_backend.entities.TicketPriority;
import com.jticket.jticket_backend.entities.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

public interface TicketService {
    Ticket createTicket(String title, String description,
                        LocalDate dueDate, MultipartFile file, UUID createdById) throws IOException;
    Page<Ticket> getMyTickets(UUID userId, Pageable pageable);
    Page<Ticket> getAssignedTickets(UUID userId, Pageable pageable);
    Page<Ticket> getAllTickets(Pageable pageable);
    Page<Ticket> getTicketsByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> searchTickets(String keyword, Pageable pageable);
    
    Ticket approveTicket(UUID ticketId, UUID reviewerId, TicketPriority priority, Integer ticketRating);
    Ticket rejectTicket(UUID ticketId, UUID reviewerId, Integer ticketRating);
    Ticket cancelTicket(UUID ticketId, UUID reviewerId, Integer ticketRating);
    
    Ticket assignTicket(UUID ticketId, UUID assignedToId, UUID assignedById);
    Ticket startWork(UUID ticketId, UUID workerId);
    Ticket completeTicket(UUID ticketId, UUID userId);
    Ticket reviveTicket(UUID ticketId, UUID adminId);
    
    Ticket rateWorker(UUID ticketId, UUID clientId, Integer rating, String feedback);
    
    void deleteTicket(UUID ticketId);
}
