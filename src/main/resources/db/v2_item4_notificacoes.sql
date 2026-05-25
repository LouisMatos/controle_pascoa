-- ============================================================
-- Item 4 — Módulo de Notificações
-- Execute uma vez no banco pascoa_db
-- ============================================================

CREATE TABLE IF NOT EXISTS templates_notificacao (
    id              BIGSERIAL PRIMARY KEY,
    evento_gatilho  VARCHAR(30)  NOT NULL,
    canal           VARCHAR(10)  NOT NULL,
    assunto         VARCHAR(200),
    corpo           TEXT         NOT NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    variaveis       VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS notificacoes_enviadas (
    id            BIGSERIAL PRIMARY KEY,
    pedido_id     BIGINT       NOT NULL REFERENCES pedidos(id),
    template_id   BIGINT       REFERENCES templates_notificacao(id),
    canal         VARCHAR(10)  NOT NULL,
    destinatario  VARCHAR(200) NOT NULL,
    data_envio    TIMESTAMP    NOT NULL DEFAULT NOW(),
    status        VARCHAR(10)  NOT NULL,
    mensagem_erro TEXT
);

CREATE TABLE IF NOT EXISTS configuracao_canal (
    id         BIGSERIAL PRIMARY KEY,
    tipo       VARCHAR(10)  NOT NULL UNIQUE,
    api_url    VARCHAR(300),
    api_key    VARCHAR(300),
    remetente  VARCHAR(200),
    ativo      BOOLEAN      NOT NULL DEFAULT FALSE,
    test_mode  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Índices úteis
CREATE INDEX IF NOT EXISTS idx_notif_enviadas_pedido   ON notificacoes_enviadas(pedido_id);
CREATE INDEX IF NOT EXISTS idx_notif_enviadas_data     ON notificacoes_enviadas(data_envio DESC);
CREATE INDEX IF NOT EXISTS idx_template_evento_canal   ON templates_notificacao(evento_gatilho, canal);

-- Templates padrão para WhatsApp (testMode ativo por default)
INSERT INTO templates_notificacao (evento_gatilho, canal, corpo, variaveis)
SELECT 'PEDIDO_CONFIRMADO', 'WHATSAPP',
       'Olá {nome}! Seu pedido #{numeroPedido} foi confirmado. Entrega prevista: {dataEntrega}. Acompanhe: {link}',
       '{nome}, {numeroPedido}, {dataEntrega}, {link}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PEDIDO_CONFIRMADO' AND canal = 'WHATSAPP'
);

INSERT INTO templates_notificacao (evento_gatilho, canal, corpo, variaveis)
SELECT 'PEDIDO_PRONTO', 'WHATSAPP',
       'Olá {nome}! Seu pedido #{numeroPedido} está PRONTO para retirada/entrega. Aguardamos você!',
       '{nome}, {numeroPedido}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PEDIDO_PRONTO' AND canal = 'WHATSAPP'
);

INSERT INTO templates_notificacao (evento_gatilho, canal, corpo, variaveis)
SELECT 'PEDIDO_ENTREGUE', 'WHATSAPP',
       'Olá {nome}! Seu pedido #{numeroPedido} foi entregue. Obrigado pela preferência! 🐣',
       '{nome}, {numeroPedido}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PEDIDO_ENTREGUE' AND canal = 'WHATSAPP'
);

INSERT INTO templates_notificacao (evento_gatilho, canal, corpo, variaveis)
SELECT 'PAGAMENTO_RECEBIDO', 'WHATSAPP',
       'Olá {nome}! Recebemos seu pagamento de {valor} referente ao pedido #{numeroPedido}. Obrigado!',
       '{nome}, {numeroPedido}, {valor}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PAGAMENTO_RECEBIDO' AND canal = 'WHATSAPP'
);

INSERT INTO templates_notificacao (evento_gatilho, canal, corpo, variaveis)
SELECT 'PRODUCAO_INICIADA', 'WHATSAPP',
       'Olá {nome}! A produção do seu pedido #{numeroPedido} foi iniciada. Entrega: {dataEntrega}.',
       '{nome}, {numeroPedido}, {dataEntrega}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PRODUCAO_INICIADA' AND canal = 'WHATSAPP'
);

-- Templates padrão para E-mail
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, variaveis)
SELECT 'PEDIDO_CONFIRMADO', 'EMAIL',
       'Pedido #{numeroPedido} confirmado!',
       '<p>Olá <strong>{nome}</strong>,</p><p>Seu pedido <strong>#{numeroPedido}</strong> foi confirmado com sucesso!</p><p>Data de entrega prevista: <strong>{dataEntrega}</strong></p><p>Acompanhe seu pedido: <a href="{link}">{link}</a></p><p>Obrigado pela preferência! 🐣</p>',
       '{nome}, {numeroPedido}, {dataEntrega}, {link}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PEDIDO_CONFIRMADO' AND canal = 'EMAIL'
);

INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, variaveis)
SELECT 'PEDIDO_PRONTO', 'EMAIL',
       'Seu pedido #{numeroPedido} está pronto!',
       '<p>Olá <strong>{nome}</strong>,</p><p>Seu pedido <strong>#{numeroPedido}</strong> está pronto para retirada ou entrega!</p><p>Qualquer dúvida, entre em contato.</p>',
       '{nome}, {numeroPedido}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'PEDIDO_PRONTO' AND canal = 'EMAIL'
);
