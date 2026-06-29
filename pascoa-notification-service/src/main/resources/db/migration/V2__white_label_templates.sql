-- FoodFlow v6 Etapa 10 — notification-service
-- Tabela de templates white-label resolvidos por (tenant_id, evento, canal),
-- com fallback para template global (tenant_id IS NULL).

ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_notif_tenant ON notificacoes(tenant_id);

CREATE TABLE IF NOT EXISTS notif_template (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  VARCHAR(50),                              -- NULL = template global default
    evento     VARCHAR(50)  NOT NULL,
    canal      VARCHAR(10)  NOT NULL CHECK (canal IN ('EMAIL','WHATSAPP','SMS')),
    assunto    VARCHAR(300),
    conteudo   TEXT         NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP
);

-- Apenas 1 template ativo por (tenant_id, evento, canal). NULL conta como mesmo "slot"
-- via COALESCE para que tenant_id NULL UNIQUE funcione no Postgres.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notif_template_slot
    ON notif_template (COALESCE(tenant_id, '__global__'), evento, canal)
    WHERE ativo = TRUE;

CREATE INDEX IF NOT EXISTS idx_notif_template_evento ON notif_template (evento, canal);

-- Seeds globais — fallback para tenants sem template próprio.
-- Variáveis disponíveis: {nome}, {nomeApp}, {numeroPedido}, {dataEntrega}, {link}, {valor}
INSERT INTO notif_template (tenant_id, evento, canal, assunto, conteudo, ativo) VALUES
  (NULL, 'PEDIDO_CONFIRMADO', 'EMAIL',
   'Pedido #{numeroPedido} confirmado — {nomeApp}',
   'Olá {nome}, seu pedido #{numeroPedido} foi confirmado. Entrega prevista para {dataEntrega}. Acompanhe em {link}.', TRUE),
  (NULL, 'PEDIDO_CONFIRMADO', 'WHATSAPP', NULL,
   'Oi {nome}! 🎉 Seu pedido #{numeroPedido} foi confirmado — entrega em {dataEntrega}. Acompanhe: {link}', TRUE),
  (NULL, 'PEDIDO_PRONTO', 'EMAIL',
   'Pedido #{numeroPedido} pronto para retirada',
   'Olá {nome}, seu pedido #{numeroPedido} está pronto! Já pode retirar.', TRUE),
  (NULL, 'PEDIDO_PRONTO', 'WHATSAPP', NULL,
   'Olá {nome}! Seu pedido #{numeroPedido} já está pronto para retirada 🎂', TRUE),
  (NULL, 'PAGAMENTO_RECEBIDO', 'EMAIL',
   'Pagamento de R$ {valor} recebido',
   'Recebemos seu pagamento de R$ {valor} referente ao pedido #{numeroPedido}. Obrigado!', TRUE),
  (NULL, 'ANIVERSARIO_CLIENTE', 'WHATSAPP', NULL,
   'Feliz aniversário, {nome}! 🎉 A {nomeApp} deseja um dia incrível. Confira mimos em {link}.', TRUE),
  (NULL, 'ORCAMENTO_EXPIRANDO', 'EMAIL',
   'Seu orçamento expira em breve',
   'Olá {nome}, seu orçamento na {nomeApp} expira em {dataEntrega}. Confirme em {link}.', TRUE)
ON CONFLICT DO NOTHING;
