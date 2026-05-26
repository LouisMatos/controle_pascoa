# Schema do Banco de Dados — Sistema Controle Páscoa

> **Fonte:** Leitura direta das migrations V1–V4 + entities Java  
> **Banco:** PostgreSQL | **Versionamento:** Flyway  
> **Atualizado em:** 2026-05-26

---

## Legenda

```
PK   → Primary Key (BIGSERIAL, gerado automaticamente)
FK   → Foreign Key
NN   → NOT NULL
UQ   → UNIQUE
DEF  → DEFAULT value
*    → Coluna de auditoria herdada de BaseEntity
[T]  → @Transient (não existe no banco, calculado em memória)
```

---

## Mapa de Relacionamentos

```
fornecedores ◄──── materias_primas (fornecedor_preferencial_id)
             ◄──── contas_pagar    (fornecedor_id, opcional)

clientes ◄──── pedidos          (cliente_id)
         ◄──── orcamentos       (cliente_id)
         ◄──── pontos_fidelidade (cliente_id)
         ◄──── notas_cliente    (cliente_id)

produtos ◄──── fichas_tecnicas        (produto_id, UNIQUE)
         ◄──── fichas_tecnicas_itens  (via ficha)
         ◄──── itens_pedido           (produto_id)
         ◄──── orcamento_itens        (produto_id)
         ◄──── ordens_producao        (produto_id)
         ◄──── checklist_qualidade    (produto_id)

pedidos ◄──── itens_pedido         (pedido_id)
        ◄──── pagamentos           (pedido_id)
        ◄──── ordens_producao      (pedido_id)
        ◄──── contas_receber       (pedido_id)
        ◄──── notificacoes_enviadas (pedido_id)
        ◄──── despesas_variaveis   (pedido_id)
        ◄──── orcamentos           (pedido_id, FK inversa — orçamento convertido)
        ◄──── pontos_fidelidade    (pedido_id, opcional)

ordens_producao ◄──── inspecao_qualidade (ordem_producao_id)

templates_notificacao ◄──── notificacoes_enviadas  (template_id)
                      ◄──── campanha_reengajamento (template_id)
```

---

## 1. Módulo Cadastro

### `fornecedores`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `nome` | VARCHAR(150) | NN | |
| `cnpj` | VARCHAR(18) | | |
| `telefone` | VARCHAR(20) | | |
| `email` | VARCHAR(150) | | |
| `observacoes` | VARCHAR(500) | | |
| `criado_em` | TIMESTAMP | NN | Auditoria BaseEntity |
| `atualizado_em` | TIMESTAMP | | Auditoria BaseEntity |
| `criado_por` | VARCHAR(100) | | Auditoria BaseEntity |
| `atualizado_por` | VARCHAR(100) | | Auditoria BaseEntity |

---

### `clientes`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `nome` | VARCHAR(150) | NN | |
| `telefone` | VARCHAR(20) | | |
| `email` | VARCHAR(150) | | |
| `endereco` | VARCHAR(300) | | |
| `cpf` | VARCHAR(14) | | |
| `excluido_em` | TIMESTAMP | | **Soft-delete** — NULL = ativo; preenchido pelo `@SQLDelete` |
| `preferencia_canal` | VARCHAR(10) | DEF `'NENHUM'` | Enum: WHATSAPP, EMAIL, AMBOS, NENHUM |
| `opt_in` | BOOLEAN | DEF `FALSE` | Consentimento para notificações |
| `data_cadastro` | TIMESTAMP | NN | ⚠️ Mapeia `criadoEm` de BaseEntity via `@AttributeOverride` |
| `atualizado_em` | TIMESTAMP | | Auditoria BaseEntity |
| `criado_por` | VARCHAR(100) | | Auditoria BaseEntity |
| `atualizado_por` | VARCHAR(100) | | Auditoria BaseEntity |

> **Soft-delete:** `DELETE` físico é interceptado e vira `UPDATE SET excluido_em = NOW()`. Todas as queries filtram automaticamente `excluido_em IS NULL` via `@SQLRestriction`.

---

### `produtos`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `nome` | VARCHAR(150) | NN | |
| `descricao` | VARCHAR(500) | | |
| `categoria` | VARCHAR(20) | NN | Enum: TRUFADO, RECHEADO, DIET, VEGANO, TRADICIONAL, ESPECIAL |
| `preco_venda` | NUMERIC(10,2) | NN | |
| `ativo` | BOOLEAN | NN, DEF `TRUE` | |
| `margem_desejada` | NUMERIC(5,2) | | % de margem desejada |
| `foto` | VARCHAR(500) | | Caminho relativo em `~/pascoa-uploads` |
| `inicio_safra` | DATE | | |
| `fim_safra` | DATE | | |
| `excluido_em` | TIMESTAMP | | **Soft-delete** — igual ao de clientes |
| `criado_em` | TIMESTAMP | NN | Auditoria BaseEntity |
| `atualizado_em` | TIMESTAMP | | Auditoria BaseEntity |
| `criado_por` | VARCHAR(100) | | Auditoria BaseEntity |
| `atualizado_por` | VARCHAR(100) | | Auditoria BaseEntity |

---

### `materias_primas`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `nome` | VARCHAR(150) | NN | |
| `unidade` | VARCHAR(5) | NN | Enum: KG, G, L, ML, UN, CX |
| `quantidade_atual` | NUMERIC(12,3) | NN, DEF `0` | Atualizado a cada movimentação de estoque |
| `quantidade_minima` | NUMERIC(12,3) | NN, DEF `0` | Abaixo disso → estoque crítico |
| `custo_unitario` | NUMERIC(10,4) | NN, DEF `0` | Custo da última compra |
| `custo_medio_ponderado` | NUMERIC(10,4) | DEF `0` | Recalculado em cada ENTRADA de estoque |
| `data_ultima_compra` | DATE | | |
| `fornecedor_preferencial_id` | BIGINT | FK → `fornecedores(id)` ON DELETE SET NULL | |
| `criado_em` | TIMESTAMP | NN | Auditoria BaseEntity |
| `atualizado_em` | TIMESTAMP | | Auditoria BaseEntity |
| `criado_por` | VARCHAR(100) | | Auditoria BaseEntity |
| `atualizado_por` | VARCHAR(100) | | Auditoria BaseEntity |

---

## 2. Módulo Ficha Técnica

### `fichas_tecnicas`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `produto_id` | BIGINT | NN, UQ, FK → `produtos(id)` | Relação 1:1 com produto |
| `rendimento` | NUMERIC(10,3) | NN | Quantidade produzida pela receita |
| `unidade_rendimento` | VARCHAR(5) | NN | Unidade do rendimento (ex: UN, KG) |
| `observacoes` | VARCHAR(500) | | |

### `fichas_tecnicas_itens`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `ficha_tecnica_id` | BIGINT | NN, FK → `fichas_tecnicas(id)` | |
| `materia_prima_id` | BIGINT | NN, FK → `materias_primas(id)` | |
| `quantidade` | NUMERIC(12,3) | NN | Quantidade do ingrediente na receita |
| — | — | UQ(`ficha_tecnica_id`, `materia_prima_id`) | Impede duplicata de ingrediente |

---

## 3. Módulo Pedidos

### `pedidos`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `cliente_id` | BIGINT | NN, FK → `clientes(id)` | |
| `data_pedido` | TIMESTAMP | NN | Preenchido via `@PrePersist` |
| `data_entrega` | DATE | | |
| `status` | VARCHAR(20) | NN, DEF `'NOVO'` | Enum: NOVO, CONFIRMADO, EM_PRODUCAO, PRONTO, ENTREGUE, CANCELADO |
| `observacoes` | VARCHAR(500) | | |
| `total_pedido` | NUMERIC(10,2) | NN, DEF `0` | Recalculado ao adicionar/remover itens |
| `token_acompanhamento` | VARCHAR(36) | UQ | UUID gerado no `@PrePersist` — para rastreamento público |
| `slot_entrega` | TIME | | Horário de entrega |
| `custo_real_calculado` | NUMERIC(10,2) | | Preenchido por `CustoRealService` |
| `margem_real_calculada` | NUMERIC(5,2) | | % calculado após custo real |

> **Sem auditoria BaseEntity** — `pedidos` não herda de BaseEntity. Usa `data_pedido` como timestamp de criação.

### `itens_pedido`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | NN, FK → `pedidos(id)` | |
| `produto_id` | BIGINT | NN, FK → `produtos(id)` | |
| `quantidade` | INTEGER | NN | |
| `preco_unitario` | NUMERIC(10,2) | NN | **Fixado no momento do pedido** — não atualiza se produto mudar de preço |
| `subtotal` | NUMERIC(12,2) | | `quantidade × preco_unitario` |

### `pagamentos`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | NN, FK → `pedidos(id)` | |
| `valor` | NUMERIC(10,2) | NN | |
| `tipo_pagamento` | VARCHAR(20) | NN | Enum: PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, FIADO |
| `data_pagamento` | DATE | NN | |
| `observacoes` | VARCHAR(300) | | |

---

## 4. Módulo Orçamentos

### `orcamentos`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `cliente_id` | BIGINT | NN, FK → `clientes(id)` | Índice: `idx_orcamento_cliente` |
| `data_criacao` | TIMESTAMP | NN, DEF `NOW()` | |
| `validade` | DATE | NN | Após esta data → expirado |
| `status` | VARCHAR(15) | NN, DEF `'PENDENTE'` | Enum: PENDENTE, APROVADO, RECUSADO, EXPIRADO. Índice: `idx_orcamento_status` |
| `total` | NUMERIC(12,2) | NN, DEF `0` | Soma dos subtotais dos itens |
| `observacoes` | VARCHAR(1000) | | |
| `token_aprovacao` | VARCHAR(36) | UQ | UUID para aprovação pública sem login |
| `pedido_id` | BIGINT | FK → `pedidos(id)` | Preenchido após conversão em pedido |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | ⚠️ Campo extra no SQL — não mapeado no Java entity |
| `criado_por` | VARCHAR(100) | | |
| `atualizado_em` | TIMESTAMP | | ⚠️ No SQL, não mapeado no Java entity |
| `atualizado_por` | VARCHAR(100) | | ⚠️ No SQL, não mapeado no Java entity |

### `orcamento_itens`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `orcamento_id` | BIGINT | NN, FK → `orcamentos(id)` ON DELETE CASCADE | |
| `produto_id` | BIGINT | NN, FK → `produtos(id)` | |
| `quantidade` | INTEGER | NN | |
| `preco_unitario` | NUMERIC(10,2) | NN | |
| `subtotal` | NUMERIC(12,2) | NN | |

---

## 5. Módulo Estoque

### `movimentacoes_estoque`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `materia_prima_id` | BIGINT | NN, FK → `materias_primas(id)` | |
| `tipo` | VARCHAR(10) | NN | Enum: ENTRADA, SAIDA, AJUSTE |
| `quantidade` | NUMERIC(12,3) | NN | |
| `saldo_apos` | NUMERIC(12,3) | NN | Saldo de `materias_primas.quantidade_atual` após a movimentação |
| `data` | TIMESTAMP | NN | |
| `motivo` | VARCHAR(500) | | |
| `usuario` | VARCHAR(100) | | Login do usuário que registrou |
| `custo_unitario` | NUMERIC(10,4) | | Preenchido em entradas; usado para recalcular custo médio |

---

## 6. Módulo Produção

### `ordens_producao`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | FK → `pedidos(id)` | Opcional — ordem pode existir sem pedido direto |
| `produto_id` | BIGINT | NN, FK → `produtos(id)` | |
| `quantidade` | INTEGER | NN | |
| `status` | VARCHAR(20) | NN, DEF `'PENDENTE'` | Enum: PENDENTE, EM_ANDAMENTO, CONCLUIDA |
| `data_abertura` | TIMESTAMP | | |
| `data_conclusao` | TIMESTAMP | | |
| `observacoes` | VARCHAR(500) | | |

---

## 7. Módulo Qualidade

### `checklist_qualidade`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `produto_id` | BIGINT | NN, FK → `produtos(id)` | Template de checklist por produto |
| `item` | VARCHAR(300) | NN | Descrição do item a verificar |
| `ordem` | INTEGER | NN, DEF `0` | Ordenação dos itens |
| `ativo` | BOOLEAN | NN, DEF `TRUE` | |

### `inspecao_qualidade`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `ordem_producao_id` | BIGINT | NN, FK → `ordens_producao(id)` | Índice: `idx_inspecao_ordem` |
| `data_inspecao` | TIMESTAMP | NN, DEF `NOW()` | |
| `inspetor` | VARCHAR(100) | NN | Login do usuário inspetor |
| `aprovado` | BOOLEAN | NN | `true` = aprovado, `false` = reprovado |
| `observacoes` | VARCHAR(1000) | | |
| `itens_verificados` | **JSONB** | | Lista dinâmica de `{item, aprovado, observacao}` — sem schema fixo |

> **JSONB:** Tipo nativo do PostgreSQL. Permite indexação e consulta por campos internos sem alterar a tabela. Não requer migration para adicionar novos itens ao checklist.

---

## 8. Módulo Financeiro

### `configuracao_financeira`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | Sempre existe 1 registro — singleton |
| `margem_desejada_padrao` | NUMERIC(5,2) | NN, DEF `30.00` | % sugerido nas fichas técnicas |
| `aliquota_simples` | NUMERIC(5,2) | NN, DEF `6.00` | % deduzido do faturamento (Simples Nacional) |
| `meta_faturamento_mensal` | NUMERIC(12,2) | NN, DEF `0` | Usada no dashboard e projeção de safra |
| `moeda` | VARCHAR(5) | NN, DEF `'BRL'` | |

### `contas_receber`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | NN, FK → `pedidos(id)` | |
| `valor_original` | NUMERIC(10,2) | NN | Total do pedido no momento da criação |
| `valor_pago` | NUMERIC(10,2) | DEF `0` | Acumulado conforme pagamentos registrados |
| `vencimento` | DATE | NN | Normalmente = data de entrega do pedido |
| `status` | VARCHAR(10) | NN, DEF `'ABERTA'` | Enum: ABERTA, PAGA, VENCIDA |
| `saldo` | — | [T] | `valor_original - valor_pago` — calculado em memória |

### `contas_pagar`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `fornecedor_id` | BIGINT | FK → `fornecedores(id)` | Opcional — pode ser despesa sem fornecedor |
| `descricao` | VARCHAR(200) | NN | |
| `valor` | NUMERIC(10,2) | NN | |
| `vencimento` | DATE | NN | |
| `status` | VARCHAR(10) | NN, DEF `'ABERTA'` | Enum: ABERTA, PAGA, VENCIDA |
| `categoria` | VARCHAR(20) | NN | Enum `CategoriaContaPagar` |

### `despesas_fixas`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `descricao` | VARCHAR(200) | NN | |
| `valor` | NUMERIC(10,2) | NN | Valor mensal (periodicidade MENSAL), ou anual rateado |
| `periodicidade` | VARCHAR(10) | NN | Enum: MENSAL, ANUAL, CUSTOMIZADA |
| `ativo` | BOOLEAN | NN, DEF `TRUE` | Inativas ignoradas no cálculo de break-even |
| `data_inicio` | DATE | NN | |
| `data_cancelamento` | DATE | | |

### `despesas_variaveis`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | NN, FK → `pedidos(id)` | ⚠️ Vinculada a um pedido específico — distinta de `gastos_variaveis` |
| `descricao` | VARCHAR(200) | NN | |
| `valor` | NUMERIC(10,2) | NN | |
| `categoria` | VARCHAR(20) | NN | Enum `CategoriaDespesaVariavel` |

---

## 9. Módulo Gastos

### `gastos_variaveis`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `descricao` | VARCHAR(200) | NN | |
| `valor` | NUMERIC(10,2) | NN | |
| `data_lancamento` | DATE | NN | |
| `categoria` | VARCHAR(30) | NN | Enum `CategoriaGasto` |
| `referencia_mes` | INTEGER | NN, CHECK 1-12 | |
| `referencia_ano` | INTEGER | NN | |
| `observacoes` | VARCHAR(500) | | |
| `comprovante_url` | VARCHAR(500) | | |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | |
| `criado_por` | VARCHAR(100) | | |

Índice: `idx_gastos_mes_ano ON (referencia_ano, referencia_mes)`

> **Atenção:** `gastos_variaveis` ≠ `despesas_variaveis`. `gastos_variaveis` é o módulo de controle de gastos operacionais por período (sem FK para pedido). `despesas_variaveis` (V1) está vinculada a um pedido específico.

### `orcamentos_gasto`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `categoria` | VARCHAR(30) | NN | Enum `CategoriaGasto` |
| `valor_orcado` | NUMERIC(10,2) | NN | Meta de gasto para o período |
| `referencia_mes` | INTEGER | NN, CHECK 1-12 | |
| `referencia_ano` | INTEGER | NN | |
| — | — | UQ(`categoria`, `referencia_mes`, `referencia_ano`) | Um orçamento por categoria por mês |

---

## 10. Módulo CRM

### `pontos_fidelidade`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `cliente_id` | BIGINT | NN, FK → `clientes(id)` | Índice: `idx_pontos_cliente` |
| `pedido_id` | BIGINT | FK → `pedidos(id)` | Opcional — pode ser lançamento manual |
| `pontos` | INTEGER | NN | Valor positivo = GANHO; negativo = RESGATADO |
| `tipo` | VARCHAR(10) | NN | Enum: GANHO, RESGATADO |
| `descricao` | VARCHAR(200) | | |
| `data_operacao` | TIMESTAMP | NN, DEF `NOW()` | |
| `data_expiracao` | DATE | | |

### `notas_cliente`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `cliente_id` | BIGINT | NN, FK → `clientes(id)` | Índice: `idx_nota_cliente` |
| `texto` | VARCHAR(2000) | NN | |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | |
| `criado_por` | VARCHAR(100) | | |

---

## 11. Módulo Notificações

### `templates_notificacao`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `evento_gatilho` | VARCHAR(30) | NN | Enum `EventoNotificacao`: PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE, PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO |
| `canal` | VARCHAR(10) | NN | Enum `CanalNotificacao`: EMAIL, WHATSAPP |
| `assunto` | VARCHAR(200) | | Usado apenas para EMAIL |
| `corpo` | TEXT | NN | Suporta placeholders: `{nome}`, `{numeroPedido}`, `{dataEntrega}`, `{link}`, `{valor}` |
| `ativo` | BOOLEAN | NN, DEF `TRUE` | |
| `variaveis` | VARCHAR(500) | | Documentação das variáveis disponíveis (referência) |

### `notificacoes_enviadas`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | NN, FK → `pedidos(id)` | |
| `template_id` | BIGINT | FK → `templates_notificacao(id)` | |
| `canal` | VARCHAR(10) | NN | |
| `destinatario` | VARCHAR(200) | NN | E-mail ou telefone |
| `data_envio` | TIMESTAMP | NN, DEF `NOW()` | |
| `status` | VARCHAR(10) | NN | Enum `StatusEnvio`: ENVIADA, FALHA |
| `mensagem_erro` | TEXT | | Preenchido quando `status = FALHA` |

### `configuracao_canal`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `tipo` | VARCHAR(10) | NN, UQ | EMAIL ou WHATSAPP |
| `api_url` | VARCHAR(300) | | URL da Evolution API (WhatsApp) |
| `api_key` | VARCHAR(300) | | Token da Evolution API |
| `remetente` | VARCHAR(200) | | Nome da instância WhatsApp ou e-mail remetente |
| `ativo` | BOOLEAN | NN, DEF `FALSE` | |
| `test_mode` | BOOLEAN | NN, DEF `TRUE` | `true` = apenas loga, não envia de verdade |

### `alertas_internos`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `mensagem` | VARCHAR(500) | NN | |
| `link` | VARCHAR(300) | | URL de ação relacionada ao alerta |
| `icone` | VARCHAR(60) | NN, DEF `'bi-bell'` | Classe Bootstrap Icon |
| `cor` | VARCHAR(20) | NN, DEF `'secondary'` | Classe de cor Bootstrap |
| `lido` | BOOLEAN | NN, DEF `FALSE` | |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | |

Índice: `idx_alerta_lido ON (lido, criado_em DESC)`

---

## 12. Módulo Segurança

### `usuarios`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `nome` | VARCHAR(255) | NN | |
| `login` | VARCHAR(60) | NN, UQ | Usado na autenticação Spring Security |
| `senha` | VARCHAR(255) | NN | Hash BCrypt |
| `role` | VARCHAR(20) | NN | Enum: ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA |
| `ativo` | BOOLEAN | NN, DEF `TRUE` | `false` = login bloqueado |

---

## 13. Tabela Órfã (SQL sem Entity Java)

### `campanha_reengajamento` ⚠️
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `nome` | VARCHAR(200) | NN | |
| `descricao` | VARCHAR(1000) | | |
| `segmento` | VARCHAR(30) | NN | |
| `canal` | VARCHAR(10) | NN | |
| `template_id` | BIGINT | FK → `templates_notificacao(id)` | |
| `data_envio` | TIMESTAMP | | |
| `status` | VARCHAR(15) | NN, DEF `'RASCUNHO'` | |
| `total_destinatarios` | INTEGER | DEF `0` | |
| `total_enviados` | INTEGER | DEF `0` | |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | |
| `criado_por` | VARCHAR(100) | | |

> **Atenção:** Tabela criada em V2 mas **sem entity Java correspondente**. Projetada para campanhas de CRM em lote, mas o módulo não foi implementado. Existe no banco, mas não é usada pela aplicação.

---

## 14. Índices

| Índice | Tabela | Colunas | Propósito |
|--------|--------|---------|-----------|
| `idx_gastos_mes_ano` | `gastos_variaveis` | `(referencia_ano, referencia_mes)` | Filtro por período no FluxoCaixa e Breakeven |
| `idx_orcamento_cliente` | `orcamentos` | `(cliente_id)` | Listagem de orçamentos por cliente |
| `idx_orcamento_status` | `orcamentos` | `(status)` | Filtro por status |
| `idx_pontos_cliente` | `pontos_fidelidade` | `(cliente_id)` | Histórico de pontos por cliente |
| `idx_inspecao_ordem` | `inspecao_qualidade` | `(ordem_producao_id)` | Inspeções por ordem de produção |
| `idx_nota_cliente` | `notas_cliente` | `(cliente_id)` | Notas por cliente |
| `idx_alerta_lido` | `alertas_internos` | `(lido, criado_em DESC)` | Busca de alertas não lidos, mais recentes primeiro |

---

## 15. Resumo de Tabelas por Módulo

| Módulo | Tabelas |
|--------|---------|
| Cadastro | `clientes`, `produtos`, `fornecedores`, `materias_primas` |
| Ficha Técnica | `fichas_tecnicas`, `fichas_tecnicas_itens` |
| Pedidos | `pedidos`, `itens_pedido`, `pagamentos` |
| Orçamentos | `orcamentos`, `orcamento_itens` |
| Estoque | `movimentacoes_estoque` |
| Produção | `ordens_producao` |
| Qualidade | `checklist_qualidade`, `inspecao_qualidade` |
| Financeiro | `configuracao_financeira`, `contas_receber`, `contas_pagar`, `despesas_fixas`, `despesas_variaveis` |
| Gastos | `gastos_variaveis`, `orcamentos_gasto` |
| CRM | `pontos_fidelidade`, `notas_cliente` |
| Notificações | `templates_notificacao`, `notificacoes_enviadas`, `configuracao_canal`, `alertas_internos` |
| Segurança | `usuarios` |
| **Órfã** | `campanha_reengajamento` (sem entity Java) |
| **Total** | **25 tabelas** |

---

## 16. Guia para Novas Migrations

Próxima versão disponível: **V5**

```sql
-- Arquivo: src/main/resources/db/migration/V5__descricao.sql

-- Adicionar coluna nullable (seguro, sem DEFAULT obrigatório)
ALTER TABLE nome_tabela ADD COLUMN nova_coluna VARCHAR(100);

-- Adicionar coluna NOT NULL (requer DEFAULT ou UPDATE prévio)
ALTER TABLE nome_tabela ADD COLUMN nova_coluna VARCHAR(100) NOT NULL DEFAULT 'valor';

-- Criar nova tabela
CREATE TABLE IF NOT EXISTS nova_tabela (
    id        BIGSERIAL PRIMARY KEY,
    ...
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);
```

> **Regra:** Nunca alterar migrations já aplicadas. Sempre criar nova versão V{N}.
