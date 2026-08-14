package com.testeverzel.eventos_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.testeverzel.eventos_api.domain.Ticket;

import jakarta.persistence.LockModeType;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByQrCodeHash(String qrCodeHash);

    // Locked so two simultaneous gate scans of the same ticket can't both pass as first use.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select t from Ticket t
            join fetch t.reservation r
            join fetch r.seat s
            join fetch s.event
            where r.user.id = :userId
            order by t.createdAt desc
            """)
    List<Ticket> findAllByUserId(@Param("userId") UUID userId);
}
