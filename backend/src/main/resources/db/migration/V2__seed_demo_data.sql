-- Dados de demonstração para avaliação: permitem percorrer o fluxo completo sem cadastrar nada.
-- Todos os usuários usam a senha "senha123" (mesmo hash BCrypt, gerado pela própria aplicação).
-- Datas são relativas ao momento da migração para que os eventos nunca fiquem no passado.
-- Não há reservas nem ingressos semeados: os assentos ficam todos livres para o avaliador.

INSERT INTO users (id, email, senha_hash, role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'organizador@demo.com', '$2a$10$zGYH4XDKA6sYLW3kJw9nCubFygi0aYxkNRKdp06AcihfupstV9sdS', 'ORGANIZER'),
    ('22222222-2222-2222-2222-222222222222', 'cliente1@demo.com',    '$2a$10$zGYH4XDKA6sYLW3kJw9nCubFygi0aYxkNRKdp06AcihfupstV9sdS', 'CUSTOMER'),
    ('33333333-3333-3333-3333-333333333333', 'cliente2@demo.com',    '$2a$10$zGYH4XDKA6sYLW3kJw9nCubFygi0aYxkNRKdp06AcihfupstV9sdS', 'CUSTOMER'),
    ('44444444-4444-4444-4444-444444444444', 'portaria@demo.com',    '$2a$10$zGYH4XDKA6sYLW3kJw9nCubFygi0aYxkNRKdp06AcihfupstV9sdS', 'GATE');

INSERT INTO events (id, titulo, sinopse, data, local, capacidade, preco_base, organizer_id, tmdb_id) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001',
     'Interestelar — Sessão IMAX',
     'Reexibição em 70mm com introdução do projecionista. Um grupo de exploradores atravessa um buraco de minhoca em busca de um novo lar para a humanidade.',
     now() + interval '12 days', 'Cine Belas Artes — Sala 1', 30, 48.00,
     '11111111-1111-1111-1111-111111111111', 157336),

    ('aaaaaaaa-0000-0000-0000-000000000002',
     'Duna: Parte Dois',
     'Pré-estreia nacional, com sessões dublada e legendada. Paul Atreides se une aos Fremen para vingar sua família.',
     now() + interval '20 days', 'Arena Multiplex — Sala 4', 24, 62.50,
     '11111111-1111-1111-1111-111111111111', 693134),

    ('aaaaaaaa-0000-0000-0000-000000000003',
     'Metrópole ao Vivo',
     'Show de encerramento da turnê, formação completa acompanhada de orquestra.',
     now() + interval '35 days', 'Teatro Municipal', 24, 120.00,
     '11111111-1111-1111-1111-111111111111', NULL);

-- Assentos gerados a partir da configuração de fileiras de cada evento.
INSERT INTO seats (event_id, fileira, numero)
SELECT cfg.event_id, cfg.fileira, numero
FROM (VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001'::uuid, 'A', 10),
    ('aaaaaaaa-0000-0000-0000-000000000001'::uuid, 'B', 10),
    ('aaaaaaaa-0000-0000-0000-000000000001'::uuid, 'C', 10),
    ('aaaaaaaa-0000-0000-0000-000000000002'::uuid, 'A', 12),
    ('aaaaaaaa-0000-0000-0000-000000000002'::uuid, 'B', 12),
    ('aaaaaaaa-0000-0000-0000-000000000003'::uuid, 'A', 6),
    ('aaaaaaaa-0000-0000-0000-000000000003'::uuid, 'B', 6),
    ('aaaaaaaa-0000-0000-0000-000000000003'::uuid, 'C', 6),
    ('aaaaaaaa-0000-0000-0000-000000000003'::uuid, 'D', 6)
) AS cfg(event_id, fileira, qtd)
CROSS JOIN LATERAL generate_series(1, cfg.qtd) AS numero;
