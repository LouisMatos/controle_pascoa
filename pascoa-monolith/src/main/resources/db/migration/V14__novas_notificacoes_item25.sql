-- ============================================================
-- Item 25: Novas Notificações
--   • data_nascimento em clientes (para aniversário)
--   • pedido_id nullable + cliente_id + orcamento_id em notificacoes_enviadas
--   • Canal SMS em configuracao_canal
--   • Templates padrão para ANIVERSARIO_CLIENTE e ORCAMENTO_EXPIRANDO
-- ============================================================

-- 1. Campo data de nascimento em clientes
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS data_nascimento DATE;

-- 2. Tornar pedido_id nullable (notificações sem pedido: aniversário, orçamento)
ALTER TABLE notificacoes_enviadas ALTER COLUMN pedido_id DROP NOT NULL;

-- 3. Referência direta ao cliente (para aniversário e notificações sem pedido)
ALTER TABLE notificacoes_enviadas
    ADD COLUMN IF NOT EXISTS cliente_id BIGINT REFERENCES clientes(id) ON DELETE SET NULL;

-- 4. Referência ao orçamento (para ORCAMENTO_EXPIRANDO)
ALTER TABLE notificacoes_enviadas
    ADD COLUMN IF NOT EXISTS orcamento_id BIGINT REFERENCES orcamentos(id) ON DELETE SET NULL;

-- 5. Índice único para idempotência de orçamento expirando
--    (apenas um alerta ENVIADO por orçamento × canal)
CREATE UNIQUE INDEX IF NOT EXISTS uq_notif_orcamento_expirando
    ON notificacoes_enviadas (orcamento_id, evento, canal)
    WHERE orcamento_id IS NOT NULL
      AND evento = 'ORCAMENTO_EXPIRANDO'
      AND status = 'ENVIADA';

-- 6. Índice de busca para aniversários (não único — pode reenviar em anos diferentes)
CREATE INDEX IF NOT EXISTS idx_notif_aniversario
    ON notificacoes_enviadas (cliente_id, evento, data_envio)
    WHERE evento = 'ANIVERSARIO_CLIENTE';

-- 7. Configuração padrão para canal SMS (inativo por padrão)
INSERT INTO configuracao_canal (tipo, api_url, api_key, remetente, ativo, test_mode)
VALUES ('SMS', '', '', '', FALSE, TRUE)
ON CONFLICT (tipo) DO NOTHING;

-- 8. Templates padrão para novos eventos
--    ANIVERSARIO_CLIENTE — EMAIL
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT 'ANIVERSARIO_CLIENTE', 'EMAIL',
       'Feliz Aniversario, {nome}!',
       '<p>Ola, {nome}!</p><p>A equipe Pascoa Artesanal deseja um feliz aniversario! 🎉</p><p>Para celebrar essa data especial, temos uma surpresa para voce. Entre em contato para saber mais!</p><p>Com carinho,<br/>Equipe Pascoa Artesanal</p>',
       TRUE,
       '{nome}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'ANIVERSARIO_CLIENTE' AND canal = 'EMAIL'
);

--    ANIVERSARIO_CLIENTE — WHATSAPP
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT 'ANIVERSARIO_CLIENTE', 'WHATSAPP',
       NULL,
       'Ola, {nome}! 🎂 A equipe Pascoa Artesanal deseja um feliz aniversario! Temos uma surpresa especial para voce — entre em contato para saber mais!',
       TRUE,
       '{nome}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'ANIVERSARIO_CLIENTE' AND canal = 'WHATSAPP'
);

--    ORCAMENTO_EXPIRANDO — EMAIL
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT 'ORCAMENTO_EXPIRANDO', 'EMAIL',
       'Seu orcamento vence em 2 dias, {nome}!',
       '<p>Ola, {nome}!</p><p>Seu orcamento <strong>#ORC-{numeroOrcamento}</strong> no valor de <strong>{valor}</strong> vence em <strong>{validade}</strong>.</p><p>Nao perca essa oportunidade! <a href="{link}">Clique aqui para aprovar seu orcamento</a>.</p>',
       TRUE,
       '{nome}, {numeroOrcamento}, {valor}, {validade}, {link}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'ORCAMENTO_EXPIRANDO' AND canal = 'EMAIL'
);

--    ORCAMENTO_EXPIRANDO — WHATSAPP
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT 'ORCAMENTO_EXPIRANDO', 'WHATSAPP',
       NULL,
       'Ola, {nome}! ⚠️ Seu orcamento *#ORC-{numeroOrcamento}* no valor de *{valor}* vence em *{validade}*. Acesse o link para aprova-lo: {link}',
       TRUE,
       '{nome}, {numeroOrcamento}, {valor}, {validade}, {link}'
WHERE NOT EXISTS (
    SELECT 1 FROM templates_notificacao
    WHERE evento_gatilho = 'ORCAMENTO_EXPIRANDO' AND canal = 'WHATSAPP'
);
