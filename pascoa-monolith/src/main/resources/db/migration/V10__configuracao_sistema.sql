-- Configuração geral do sistema (singleton — sempre id = 1)
CREATE TABLE IF NOT EXISTS configuracao_sistema (
    id                    BIGINT PRIMARY KEY DEFAULT 1,
    modo_manutencao       BOOLEAN NOT NULL DEFAULT FALSE,
    mensagem_manutencao   VARCHAR(500) DEFAULT 'O sistema está temporariamente indisponível para manutenção.',
    previsao_retorno      VARCHAR(100) DEFAULT NULL,
    atualizado_em         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Garante que a linha singleton existe sempre
INSERT INTO configuracao_sistema (id, modo_manutencao)
VALUES (1, FALSE)
ON CONFLICT (id) DO NOTHING;
