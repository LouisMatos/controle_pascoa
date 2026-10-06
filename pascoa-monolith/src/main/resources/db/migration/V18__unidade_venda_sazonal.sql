ALTER TABLE produtos ADD COLUMN unidade_venda VARCHAR(10) NOT NULL DEFAULT 'UNIDADE';
ALTER TABLE produtos ADD COLUMN sazonal BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE produtos p
SET unidade_venda = CASE f.unidade_rendimento
        WHEN 'KG' THEN 'KG'
        WHEN 'CX' THEN 'PACOTE'
        ELSE 'UNIDADE'
    END
FROM fichas_tecnicas f
WHERE f.produto_id = p.id;

UPDATE produtos SET sazonal = TRUE WHERE inicio_safra IS NOT NULL OR fim_safra IS NOT NULL;

ALTER TABLE fichas_tecnicas ALTER COLUMN unidade_rendimento DROP NOT NULL;
