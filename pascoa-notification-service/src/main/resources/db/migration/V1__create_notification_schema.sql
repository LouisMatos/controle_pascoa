-- Notification Service — Schema V1
-- Banco: pascoa_notifications

CREATE TABLE notificacoes (
    id             BIGSERIAL       PRIMARY KEY,
    destinatario   VARCHAR(200)    NOT NULL,
    canal          VARCHAR(10)     NOT NULL CHECK (canal IN ('EMAIL','WHATSAPP','SMS')),
    assunto        VARCHAR(300),
    conteudo       TEXT            NOT NULL,
    status         VARCHAR(10)     NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','ENVIADA','FALHOU')),
    evento         VARCHAR(50),
    referencia_id  VARCHAR(50),
    erro_mensagem  VARCHAR(500),
    criado_em      TIMESTAMP       NOT NULL DEFAULT NOW(),
    enviado_em     TIMESTAMP
);

CREATE INDEX idx_notif_status      ON notificacoes(status);
CREATE INDEX idx_notif_referencia  ON notificacoes(referencia_id);
CREATE INDEX idx_notif_criado_em   ON notificacoes(criado_em DESC);
CREATE INDEX idx_notif_evento      ON notificacoes(evento);
