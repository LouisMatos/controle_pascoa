CREATE TABLE IF NOT EXISTS lojas (
    id        BIGSERIAL PRIMARY KEY,
    nome      VARCHAR(150) NOT NULL,
    criada_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO lojas (id, nome) VALUES (1, 'Loja Padrão') ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('lojas', 'id'), GREATEST((SELECT MAX(id) FROM lojas), 1));

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'alertas_internos', 'audit_log', 'checklist_qualidade', 'clientes',
        'configuracao_canal', 'configuracao_financeira', 'contas_pagar', 'contas_receber',
        'despesas_fixas', 'despesas_variaveis', 'fichas_tecnicas', 'fichas_tecnicas_itens',
        'fornecedores', 'gastos_variaveis', 'inspecao_qualidade', 'itens_pedido',
        'materias_primas', 'movimentacoes_estoque', 'notas_cliente', 'notificacoes_enviadas',
        'orcamento_itens', 'orcamentos', 'orcamentos_gasto', 'ordens_producao',
        'pagamentos', 'pedidos', 'pontos_fidelidade', 'produtos', 'templates_notificacao',
        'usuarios'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)', t);
        EXECUTE format('CREATE INDEX idx_%s_loja_id ON %I (loja_id)', t, t);
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['configuracao_canal', 'orcamentos_gasto'] LOOP
        FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = t::regclass AND contype = 'u' LOOP
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', t, r.conname);
        END LOOP;
    END LOOP;
END $$;

ALTER TABLE configuracao_canal ADD CONSTRAINT uq_configuracao_canal_loja_tipo UNIQUE (loja_id, tipo);
ALTER TABLE orcamentos_gasto ADD CONSTRAINT uq_orcamentos_gasto_loja_cat_mes_ano
    UNIQUE (loja_id, categoria, referencia_mes, referencia_ano);
