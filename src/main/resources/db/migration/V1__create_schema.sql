CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('ORGANIZER', 'CUSTOMER', 'GATE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo        VARCHAR(255) NOT NULL,
    sinopse       TEXT,
    data          TIMESTAMPTZ NOT NULL,
    local         VARCHAR(255) NOT NULL,
    capacidade    INTEGER NOT NULL CHECK (capacidade > 0),
    preco_base    NUMERIC(10, 2) NOT NULL CHECK (preco_base >= 0),
    organizer_id  UUID NOT NULL REFERENCES users (id),
    tmdb_id       BIGINT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_organizer_id ON events (organizer_id);

CREATE TABLE seats (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id  UUID NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    fileira   VARCHAR(10) NOT NULL,
    numero    INTEGER NOT NULL,
    status    VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD')),
    version   BIGINT NOT NULL DEFAULT 0,
    UNIQUE (event_id, fileira, numero)
);

CREATE INDEX idx_seats_event_id_status ON seats (event_id, status);

CREATE TABLE reservations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seat_id     UUID NOT NULL REFERENCES seats (id),
    user_id     UUID NOT NULL REFERENCES users (id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservations_seat_id ON reservations (seat_id);
CREATE INDEX idx_reservations_user_id ON reservations (user_id);
CREATE INDEX idx_reservations_status_expires_at ON reservations (status, expires_at);

-- Only one active (not yet resolved) reservation may exist per seat at a time.
CREATE UNIQUE INDEX uq_reservations_active_seat ON reservations (seat_id)
    WHERE status IN ('PENDING', 'CONFIRMED');

CREATE TABLE tickets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id  UUID NOT NULL UNIQUE REFERENCES reservations (id),
    qr_code_hash    VARCHAR(255) NOT NULL UNIQUE,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
