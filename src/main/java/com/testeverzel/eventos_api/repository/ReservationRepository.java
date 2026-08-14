package com.testeverzel.eventos_api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.testeverzel.eventos_api.domain.Reservation;
import com.testeverzel.eventos_api.domain.enums.ReservationStatus;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // Locked so a concurrent confirm/expire on the same reservation can't lost-update each other.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant instant);
}
