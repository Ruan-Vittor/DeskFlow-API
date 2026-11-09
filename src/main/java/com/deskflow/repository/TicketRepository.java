package com.deskflow.repository;

import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"requester", "technician"})
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"requester", "technician"})
    @Query(value = """
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """, countQuery = """
            SELECT COUNT(t) FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """)
    Page<Ticket> searchAll(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "technician"})
    @Query(value = """
            SELECT t FROM Ticket t
            WHERE t.requester.id = :requesterId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """, countQuery = """
            SELECT COUNT(t) FROM Ticket t
            WHERE t.requester.id = :requesterId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """)
    Page<Ticket> searchByRequester(
            @Param("requesterId") Long requesterId,
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "technician"})
    @Query(value = """
            SELECT t FROM Ticket t
            WHERE (t.technician.id = :userId
                   OR t.technician IS NULL
                   OR t.requester.id = :userId)
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """, countQuery = """
            SELECT COUNT(t) FROM Ticket t
            WHERE (t.technician.id = :userId
                   OR t.technician IS NULL
                   OR t.requester.id = :userId)
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
            """)
    Page<Ticket> searchForTechnician(
            @Param("userId") Long userId,
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            Pageable pageable
    );
}
