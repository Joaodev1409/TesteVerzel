package com.testeverzel.eventos_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.testeverzel.eventos_api.domain.Event;
import com.testeverzel.eventos_api.domain.Reservation;
import com.testeverzel.eventos_api.domain.Seat;
import com.testeverzel.eventos_api.domain.User;
import com.testeverzel.eventos_api.domain.enums.ReservationStatus;
import com.testeverzel.eventos_api.domain.enums.Role;
import com.testeverzel.eventos_api.domain.enums.SeatStatus;
import com.testeverzel.eventos_api.exception.SeatNotAvailableException;
import com.testeverzel.eventos_api.repository.EventRepository;
import com.testeverzel.eventos_api.repository.ReservationRepository;
import com.testeverzel.eventos_api.repository.SeatRepository;
import com.testeverzel.eventos_api.repository.UserRepository;

@Testcontainers
@SpringBootTest
class ReservationServiceConcurrencyTest {

    private static final int CONCURRENT_ATTEMPTS = 10;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("eventos_test")
            .withUsername("eventos")
            .withPassword("eventos");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // one Hikari connection per concurrent thread, plus headroom for the test/scheduler threads
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> CONCURRENT_ATTEMPTS + 5);
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private UUID eventId;
    private UUID seatId;
    private List<UUID> customerIds;

    @BeforeEach
    void setUp() {
        User organizer = userRepository.save(User.builder()
                .email("organizer@test.com")
                .senhaHash("hash")
                .role(Role.ORGANIZER)
                .build());

        Event event = eventRepository.save(Event.builder()
                .titulo("Concurrency Test Event")
                .data(OffsetDateTime.now().plusDays(30))
                .local("Test Venue")
                .capacidade(100)
                .precoBase(new BigDecimal("50.00"))
                .organizer(organizer)
                .build());
        eventId = event.getId();

        Seat seat = seatRepository.save(Seat.builder()
                .event(event)
                .fileira("A")
                .numero(1)
                .status(SeatStatus.AVAILABLE)
                .build());
        seatId = seat.getId();

        customerIds = IntStream.range(0, CONCURRENT_ATTEMPTS)
                .mapToObj(i -> userRepository.save(User.builder()
                        .email("customer" + i + "@test.com")
                        .senhaHash("hash")
                        .role(Role.CUSTOMER)
                        .build()).getId())
                .toList();
    }

    @Test
    void onlyOneConcurrentHoldSucceedsForTheSameSeat() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENT_ATTEMPTS);

        List<Callable<Reservation>> tasks = customerIds.stream()
                .<Callable<Reservation>>map(customerId -> () -> {
                    barrier.await();
                    return reservationService.holdSeat(eventId, seatId, customerId);
                })
                .toList();

        List<Future<Reservation>> futures;
        try {
            futures = executor.invokeAll(tasks, 30, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        int successCount = 0;
        int seatNotAvailableCount = 0;
        for (Future<Reservation> future : futures) {
            try {
                Reservation reservation = future.get();
                assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
                successCount++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(SeatNotAvailableException.class);
                seatNotAvailableCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(seatNotAvailableCount).isEqualTo(CONCURRENT_ATTEMPTS - 1);

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);

        Seat updatedSeat = seatRepository.findById(seatId).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }
}
