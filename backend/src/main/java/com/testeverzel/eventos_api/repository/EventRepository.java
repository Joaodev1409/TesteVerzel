package com.testeverzel.eventos_api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.testeverzel.eventos_api.domain.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByOrganizerIdOrderByDataAsc(UUID organizerId);
}
