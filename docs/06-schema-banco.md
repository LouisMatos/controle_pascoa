# Schema do Banco de Dados — Sistema Controle Páscoa

> **Fonte:** Migrations V1–V14 do monólito + V1 de cada microsserviço  
> **Bancos:** 10 PostgreSQL (1 monólito + 9 microsserviços) | **Versionamento:** Flyway  
> **Atualizado em:** 2026-05-29

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

clientes ◄──── pedidos              (cliente_id)
         ◄──── orcamentos           (cliente_id)
         ◄──── pontos_fidelidade    (cliente_id)
         ◄──── notas_cliente        (cliente_id)
         ◄──── notificacoes_enviadas (cliente_id, opcional — aniversário/sem pedido)

produtos ◄──── fichas_tecnicas        (produto_id, UNIQUE)
         ◄──── fichas_tecnicas_itens  (via ficha)
         ◄──── itens_pedido           (produto_id)
         ◄──── orcamento_itens        (produto_id)
         ◄──── ordens_producao        (produto_id)
         ◄──── checklist_qualidade    (produto_id)

pedidos ◄──── itens_pedido          (pedido_id)
        ◄──── pagamentos            (pedido_id)
        ◄──── ordens_producao       (pedido_id)
        ◄──── contas_receber        (pedido_id)
        ◄──── notificacoes_enviadas  (pedido_id, nullable — V14)
        ◄──── despesas_variaveis    (pedido_id)
        ◄──── orcamentos            (pedido_id, FK inversa — orçamento convertido)
        ◄──── pontos_fidelidade     (pedido_id, opcional)
        ◄──── gastos_variaveis      (pedido_id, opcional — V12)

orcamentos ◄──── notificacoes_enviadas (orcamento_id, opcional — V14)

ordens_producao ◄──── inspecao_qualidade (ordem_producao_id)

templates_notificacao ◄──── notificacoes_enviadas  (template_id)
                      ◄──── campanha_reengajamento (template_id)

usuarios ◄──── password_reset_token (usuario_id — V11)

audit_log           — tabela autônoma (sem FK, registra ações)
configuracao_sistema — tabela singleton (id = 1 fixo)
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
| `data_consentimento` | TIMESTAMP | | **V9/LGPD** — momento em que o cliente deu opt-in explícito |
| `anonimizado` | BOOLEAN | NN, DEF `FALSE` | **V9/LGPD** — `true` quando dados pessoais foram anonimizados a pedido |
| `segmento` | VARCHAR(20) | DEF `'NOVO'` | **V13** — Enum: NOVO, VIP, FREQUENTE, INATIVO. Recalculado diariamente às 02h |
| `data_nascimento` | DATE | | **V14** — usado pelo job de aniversário (`@Scheduled` diário às 08h) |

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
| `desconsiderar_no_custo` | BOOLEAN | NN, DEF `FALSE` | **V12** — `true` quando pedido vinculado é cancelado; ignorado nos relatórios |
| `pedido_id` | BIGINT | FK → `pedidos(id)` ON DELETE SET NULL | **V12** — optional; vincula gasto a pedido específico para desconto automático |

Índices: `idx_gastos_mes_ano ON (referencia_ano, referencia_mes)` | `idx_gasto_pedido ON (pedido_id) WHERE pedido_id IS NOT NULL`

> **Atenção:** `gastos_variaveis` ≠ `despesas_variaveis`. `gastos_variaveis` é o módulo de controle de gastos operacionais por período. `despesas_variaveis` (V1) está vinculada obrigatoriamente a um pedido específico.

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
| `evento_gatilho` | VARCHAR(30) | NN | Enum `EventoNotificacao`: PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE, PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO, **ANIVERSARIO_CLIENTE** (V14), **ORCAMENTO_EXPIRANDO** (V14) |
| `canal` | VARCHAR(10) | NN | Enum `CanalNotificacao`: EMAIL, WHATSAPP, **SMS** (V14) |
| `assunto` | VARCHAR(200) | | Usado apenas para EMAIL |
| `corpo` | TEXT | NN | Suporta placeholders: `{nome}`, `{numeroPedido}`, `{dataEntrega}`, `{link}`, `{valor}`, `{numeroOrcamento}`, `{validade}` |
| `ativo` | BOOLEAN | NN, DEF `TRUE` | |
| `variaveis` | VARCHAR(500) | | Documentação das variáveis disponíveis (referência) |

### `notificacoes_enviadas`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `pedido_id` | BIGINT | **nullable** (V14), FK → `pedidos(id)` | NULL em notificações de aniversário e orçamento |
| `template_id` | BIGINT | FK → `templates_notificacao(id)` | |
| `canal` | VARCHAR(10) | NN | |
| `destinatario` | VARCHAR(200) | NN | E-mail ou telefone |
| `data_envio` | TIMESTAMP | NN, DEF `NOW()` | |
| `status` | VARCHAR(10) | NN | Enum `StatusEnvio`: ENVIADA, FALHA |
| `mensagem_erro` | TEXT | | Preenchido quando `status = FALHA` |
| `evento` | VARCHAR(40) | | **V12** — nome do `EventoNotificacao`; usado no índice de idempotência |
| `cliente_id` | BIGINT | FK → `clientes(id)` ON DELETE SET NULL | **V14** — referência direta para aniversário e notificações sem pedido |
| `orcamento_id` | BIGINT | FK → `orcamentos(id)` ON DELETE SET NULL | **V14** — referência para `ORCAMENTO_EXPIRANDO` |

**Índices de idempotência:**
- `uq_notif_pedido_evento_canal` — UNIQUE parcial em `(pedido_id, evento, canal)` WHERE `status = 'ENVIADA'`
- `uq_notif_orcamento_expirando` — UNIQUE parcial em `(orcamento_id, evento, canal)` WHERE `evento = 'ORCAMENTO_EXPIRANDO' AND status = 'ENVIADA'`
- `idx_notif_aniversario` — em `(cliente_id, evento, data_envio)` WHERE `evento = 'ANIVERSARIO_CLIENTE'`

### `configuracao_canal`
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `tipo` | VARCHAR(10) | NN, UQ | EMAIL, WHATSAPP ou **SMS** (V14) |
| `api_url` | VARCHAR(300) | | URL da Evolution API (WhatsApp) ou endpoint webhook (SMS) |
| `api_key` | VARCHAR(300) | | Token da Evolution API ou Bearer token SMS |
| `remetente` | VARCHAR(200) | | Instância WhatsApp, e-mail remetente, ou sender ID SMS |
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
| `email` | VARCHAR(150) | | **V11** — usado para envio do link de recuperação de senha |

### `password_reset_token` (V11)
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `usuario_id` | BIGINT | NN, FK → `usuarios(id)` ON DELETE CASCADE | |
| `token` | VARCHAR(36) | NN, UQ | UUID aleatório |
| `expira_em` | TIMESTAMP | NN | 30 minutos após geração |
| `usado` | BOOLEAN | NN, DEF `FALSE` | Invalidado após primeiro uso |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | |

Índices: `idx_prt_token ON (token)` | `idx_prt_usuario ON (usuario_id)`

---

## 12-B. Tabelas de Infraestrutura Transversal

### `audit_log` (V8)
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGSERIAL | PK | |
| `usuario` | VARCHAR(100) | NN | Login do usuário que executou a ação |
| `acao` | VARCHAR(100) | NN | Tipo da ação (ex: CANCELAR_PEDIDO) |
| `entidade_tipo` | VARCHAR(100) | | Nome da classe/entidade alvo |
| `entidade_id` | BIGINT | | ID do registro alvo |
| `detalhes` | VARCHAR(1000) | | Informações adicionais em texto livre |
| `criado_em` | TIMESTAMP | NN, DEF `NOW()` | |

Índices: `idx_audit_log_criado_em ON (criado_em DESC)` | `idx_audit_log_usuario ON (usuario)` | `idx_audit_log_entidade ON (entidade_tipo, entidade_id)`

### `configuracao_sistema` (V10)
| Coluna | Tipo | Restrições | Notas |
|--------|------|-----------|-------|
| `id` | BIGINT | PK, DEF `1` | **Singleton** — sempre existe uma única linha com id = 1 |
| `modo_manutencao` | BOOLEAN | NN, DEF `FALSE` | Quando `true`, `MaintenanceFilter` bloqueia todas as rotas não-admin |
| `mensagem_manutencao` | VARCHAR(500) | DEF `'Sistema indisponível...'` | Exibida na tela de manutenção |
| `previsao_retorno` | VARCHAR(100) | | Texto livre (ex: "17h00") exibido na tela de manutenção |
| `atualizado_em` | TIMESTAMP | NN, DEF `NOW()` | Atualizado pelo `SistemaController` |

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

| Índice | Tabela | Colunas / Condição | Propósito |
|--------|--------|--------------------|-----------|
| `idx_gastos_mes_ano` | `gastos_variaveis` | `(referencia_ano, referencia_mes)` | Filtro por período |
| `idx_gasto_pedido` | `gastos_variaveis` | `(pedido_id)` WHERE NOT NULL | Join ao cancelar pedido |
| `idx_orcamento_cliente` | `orcamentos` | `(cliente_id)` | Listagem por cliente |
| `idx_orcamento_status` | `orcamentos` | `(status)` | Filtro por status |
| `idx_pontos_cliente` | `pontos_fidelidade` | `(cliente_id)` | Histórico de pontos |
| `idx_inspecao_ordem` | `inspecao_qualidade` | `(ordem_producao_id)` | Inspeções por ordem |
| `idx_nota_cliente` | `notas_cliente` | `(cliente_id)` | Notas por cliente |
| `idx_alerta_lido` | `alertas_internos` | `(lido, criado_em DESC)` | Alertas não lidos |
| `idx_audit_log_criado_em` | `audit_log` | `(criado_em DESC)` | Histórico cronológico |
| `idx_audit_log_usuario` | `audit_log` | `(usuario)` | Ações por usuário |
| `idx_audit_log_entidade` | `audit_log` | `(entidade_tipo, entidade_id)` | Histórico de uma entidade |
| `idx_prt_token` | `password_reset_token` | `(token)` | Busca por UUID |
| `idx_prt_usuario` | `password_reset_token` | `(usuario_id)` | Tokens de um usuário |
| `uq_notif_pedido_evento_canal` | `notificacoes_enviadas` | `(pedido_id, evento, canal)` WHERE `ENVIADA` | Idempotência por pedido |
| `uq_notif_orcamento_expirando` | `notificacoes_enviadas` | `(orcamento_id, evento, canal)` WHERE `ORCAMENTO_EXPIRANDO + ENVIADA` | Idempotência orçamento expirando |
| `idx_notif_aniversario` | `notificacoes_enviadas` | `(cliente_id, evento, data_envio)` WHERE `ANIVERSARIO_CLIENTE` | Busca de aniversários enviados |

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
| Segurança | `usuarios`, `password_reset_token` |
| Infraestrutura | `audit_log`, `configuracao_sistema` |
| **Órfã** | `campanha_reengajamento` (sem entity Java) |
| **Total** | **29 tabelas** |

> **Nota V14:** `notificacoes_enviadas` ganhou `pedido_id` nullable + `cliente_id` + `orcamento_id` para suportar notificações proativas sem pedido (aniversário, orçamento expirando).

---

## 16. Guia para Novas Migrations

Próxima versão disponível: **V15**

```sql
-- Arquivo: src/main/resources/db/migration/V15__descricao.sql

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

---

## Schemas dos Microsserviços v5

Cada microsserviço possui seu próprio banco e migration `V1__create_{servico}_schema.sql`.

### pascoa_auth — `pascoa-auth-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `usuarios` | id, login (unique), password_hash, ativo, totp_ativado, totp_secret, tentativas_falhas | idx em login |
| `usuario_roles` | usuario_id (FK), role | PK composta |

### pascoa_customers — `pascoa-customer-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `clientes` | id, nome, email, telefone, data_nascimento, ativo, criado_em | idx em email, ativo |

### pascoa_inventory — `pascoa-inventory-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `materias_primas` | id, nome, unidade, quantidade_estoque, estoque_minimo, fornecedor_id, ativo | idx estoque crítico (parcial) |
| `movimentacoes_estoque` | id, materia_prima_id (FK), tipo (ENTRADA/SAIDA), quantidade, observacao, data_movimentacao | idx por materia_prima + data |

### pascoa_products — `pascoa-product-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `produtos` | id, nome, descricao, preco, categoria, foto_url, disponivel, ativo | idx em ativo, disponivel, categoria |

### pascoa_orders — `pascoa-order-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `pedidos` | id, cliente_id, nome_cliente, status, forma_pagamento, observacao, token_rastreamento (unique) | idx em status, cliente_id, token |
| `itens_pedido` | id, pedido_id (FK cascade), produto_id, nome_produto, preco_unitario, quantidade | idx em pedido_id |

### pascoa_production — `pascoa-production-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `ordens_producao` | id, pedido_id (unique), nome_cliente, status, data_previsao, observacoes | idx em status, data_previsao (parcial) |
| `itens_ordem` | id, ordem_id (FK cascade), produto_id, nome_produto, quantidade | idx em ordem_id |

### pascoa_financial — `pascoa-financial-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `lancamentos` | id, tipo (RECEITA/DESPESA), categoria, descricao, valor, data, referencia_id, origem | idx em data, mes/ano (expressão), tipo, referencia+origem |

### pascoa_notifications — `pascoa-notification-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `notificacoes` | id, destinatario, canal, assunto, conteudo, status, evento, referencia_id, erro_mensagem, criado_em, enviado_em | idx em status, referencia_id, criado_em DESC |

### pascoa_analytics — `pascoa-analytics-service`

| Tabela | Colunas principais | Índices |
|--------|-------------------|---------|
| `registros_venda` | id, pedido_id, cliente_id, produto_id, nome_produto, quantidade, valor_total, data_venda, ano, mes | idx em ano, ano+mes, produto_id+ano, cliente_id+ano |

### Próximo número de migration por serviço

| Serviço | Última migration | Próxima |
|---------|----------------|---------|
| pascoa-monolith | V13 | **V14** |
| pascoa-auth-service | V1 | **V2** |
| pascoa-customer-service | V1 | **V2** |
| pascoa-inventory-service | V1 | **V2** |
| pascoa-product-service | V1 | **V2** |
| pascoa-order-service | V1 | **V2** |
| pascoa-production-service | V1 | **V2** |
| pascoa-financial-service | V1 | **V2** |
| pascoa-notification-service | V1 | **V2** |
| pascoa-analytics-service | V1 | **V2** |
