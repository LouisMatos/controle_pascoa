# Protocolo de Manutenção da Documentação

> **Quando executar:** Ao final de toda sessão em que código foi criado ou alterado.  
> **Atualizado em:** 2026-05-29

---

## Procedimento de Fechamento de Sessão

Antes de encerrar qualquer sessão que produziu mudanças no código, executar os passos abaixo na ordem.

### Passo 1 — Mapear o que mudou

```bash
git diff --name-only HEAD
git status --short
```

Identificar os arquivos alterados e classificá-los:

| Se o arquivo alterado for... | Docs potencialmente impactadas |
|------------------------------|-------------------------------|
| `src/main/resources/db/migration/V*.sql` (monólito) | `06-schema-banco.md` |
| `pascoa-{svc}/src/main/resources/db/migration/V*.sql` | `06-schema-banco.md` (seção microsserviços) |
| `**/entity/*.java` ou `**/domain/model/*.java` | `02-arquitetura-tecnica.md`, `06-schema-banco.md` |
| `**/controller/*.java` ou `**/adapter/in/rest/*.java` | `04-rotas-endpoints.md` |
| `config/SecurityConfig.java` ou `**/config/SecurityConfig.java` | `04-rotas-endpoints.md`, `01-infraestrutura.md` |
| `**/service/*.java` ou `**/usecase/*.java` | `03-fluxos-negocio.md` |
| `pom.xml` (nova dependência) | `01-infraestrutura.md` |
| `application.properties` ou `application.yml` | `01-infraestrutura.md` |
| `docker-compose.yml` ou `infra/**` | `01-infraestrutura.md` |
| `fragments/layout.html` (novo item de menu) | `04-rotas-endpoints.md` |
| Novo módulo monólito | `02-arquitetura-tecnica.md`, `03-fluxos-negocio.md`, `04-rotas-endpoints.md`, `05-estado-implementacao.md` |
| Novo microsserviço | `00-indice.md`, `01-infraestrutura.md`, `02-arquitetura-tecnica.md`, `04-rotas-endpoints.md`, `05-estado-implementacao.md`, `06-schema-banco.md` |
| `CLAUDE.md` (roadmap) | Atualizar quando items do roadmap concluídos |

---

### Passo 2 — Atualizar `05-estado-implementacao.md`

Este é o documento que mais envelhece entre sessões. Atualizar sempre que:

- Um item do roadmap for concluído → mudar de `❌` para `✅`
- Um item do roadmap for parcialmente concluído → mudar para `⚠️` com nota
- Um bug conhecido for corrigido → remover da seção 16 (Bugs)
- Um novo bug for descoberto → adicionar na seção 16 com arquivo e linha
- Um novo gap for identificado → adicionar na seção correspondente

**Template de entrada na seção de bugs:**
```markdown
### 🐛 Bug: Descrição curta

**Arquivo:** `caminho/do/arquivo.java:numero_linha`

**Problema:** O que está errado e qual o impacto.

**Correção necessária:** O que deve ser feito para corrigir.
```

---

### Passo 3 — Verificar por tipo de mudança

#### Nova tabela ou coluna no banco

Atualizar `06-schema-banco.md`:
- Adicionar tabela nova na seção do módulo correspondente (formato padrão com | Coluna | Tipo | Restrições | Notas |)
- Adicionar FKs novas no mapa de relacionamentos (seção 1)
- Adicionar índices novos na seção 14
- Atualizar contagem total na seção 15
- Atualizar "próxima versão disponível" na seção 16 (Guia de Migrations)

#### Nova rota HTTP

Atualizar `04-rotas-endpoints.md`:
- Adicionar linha na tabela do módulo correto
- Indicar autenticação (`🔓` ou `🔐`) e role necessária
- Atualizar contagem de rotas no resumo final (seção 22)

#### Nova lógica de negócio ou fluxo

Atualizar `03-fluxos-negocio.md`:
- Se for um novo fluxo → adicionar seção numerada
- Se for modificação de fluxo existente → atualizar a seção correspondente
- Atualizar tabela de regras de negócio críticas (seção 13) se regra nova for adicionada

#### Nova entidade ou campo em entidade existente

Atualizar `02-arquitetura-tecnica.md`:
- Seção do módulo: adicionar campo na listagem da entidade
- Se for nova relação entre módulos → atualizar diagrama de dependências (seção 20)

#### Nova dependência no `pom.xml`

Atualizar `01-infraestrutura.md`:
- Seção 1 (Stack Tecnológica): adicionar linha na tabela
- Seção 3 (Gerenciamento de Dependências): adicionar entrada

#### Novo padrão de código descoberto

Atualizar `07-convencoes-desenvolvimento.md`:
- Se for um padrão recorrente novo → adicionar na seção correspondente
- Se corrigir um padrão errado → atualizar a seção e registrar o motivo

---

### Passo 4 — Atualizar o índice se necessário

Se um documento novo foi criado durante a sessão, adicionar entrada em `docs/00-indice.md` e em `CLAUDE.md` (tabela de documentos).

---

### Passo 5 — Atualizar `CLAUDE.md` se o roadmap mudou

Se itens do roadmap foram concluídos ou iniciados, atualizar a seção **"Ordem de Implementação Pendente"** no `CLAUDE.md` com o novo status (✅/⏳).

---

## Guia Rápido de Decisão

```
Mudei código? 
  ├─ Sim
  │   ├─ Completei um item do roadmap?   → atualizar 05-estado e CLAUDE.md
  │   ├─ Corrigi um bug documentado?     → remover de 05-estado-implementacao.md
  │   ├─   Criei nova tabela/coluna?     → atualizar 06-schema-banco.md
  │   ├─ Criei nova rota?                → atualizar 04-rotas-endpoints.md
  │   ├─ Criei novo módulo/fluxo?        → atualizar 02-arquitetura e 03-fluxos
  │   └─ Descobri novo padrão?           → atualizar 07-convencoes
  └─ Não → nenhuma atualização necessária
```

---

## O que NÃO precisa atualizar a documentação

- Refatorações internas sem mudança de comportamento externo
- Correções de bug que não mudam nenhuma interface pública, rota ou schema
- Alterações em templates HTML puramente estéticas (cores, espaçamento)
- Adição de logs ou comentários
- Atualizações de testes sem mudança no código de produção

---

## Frequência de Revisão Completa

A cada **5 sessões de desenvolvimento**, fazer uma revisão completa de todos os documentos para garantir que nada ficou desatualizado. Verificar especialmente:

1. `05-estado-implementacao.md` — o mais volátil
2. `06-schema-banco.md` — crítico para migrations
3. `04-rotas-endpoints.md` — importante para permissões
4. `CLAUDE.md` — carregado em todo início de sessão
