# 09 — GitFlow & CI/CD Pipeline

## Modelo de branches

```
main          ←─ produção (tagged releases, protegida)
 │
 └── develop  ←─ integração (branch base para features, protegida)
       │
       ├── feat/descricao       ←─ novas funcionalidades
       ├── fix/descricao        ←─ correções (não críticas)
       ├── refactor/descricao   ←─ refatorações
       ├── chore/descricao      ←─ manutenção/deps/config
       │
       └── release/v1.2.0      ←─ release candidate (branch de develop)
             │
             └── [PR auto-aberto → main após CI passar]

main
 └── hotfix/v1.2.1             ←─ correção crítica (branch de main)
       └── [PRs auto-abertos → main E develop após CI passar]
```

---

## Fluxo completo por tipo de branch

### Feature / Fix / Refactor

```
1. git checkout develop
2. git pull origin develop
3. git checkout -b feat/minha-feature
4. [desenvolver, commits]
5. git push origin feat/minha-feature
   → CI dispara: ci-feature.yml (lint + testes, ~5 min)
6. Abrir PR manualmente: feat/minha-feature → develop
   → CI dispara novamente: ci-feature.yml (agora no contexto do PR)
7. Code review + aprovação
8. Merge (squash ou merge commit)
```

### Release

```
1. git checkout develop && git pull
2. git checkout -b release/v1.2.0
3. [ajustes finais: bump versão nos pom.xml, CHANGELOG]
4. git push origin release/v1.2.0
   → CI dispara: ci-release.yml (lint + testes + security + sonar + build, ~20 min)
   → Após todos passarem: PR automático release/v1.2.0 → main é aberto
5. Code review do PR auto-aberto
6. Merge PR → main
   → release.yml dispara: semver bump, tag git, GitHub Release
```

### Hotfix

```
1. git checkout main && git pull
2. git checkout -b hotfix/descricao-do-bug
3. [corrigir, testar localmente]
4. git push origin hotfix/descricao-do-bug
   → CI dispara: ci-hotfix.yml (lint + testes + security + build, ~15 min)
   → Após passar: 2 PRs auto-abertos:
       • hotfix/descricao → main   (label: hotfix, urgent)
       • hotfix/descricao → develop (label: hotfix, backport)
5. Revisar e mergear PR → main primeiro
   → release.yml dispara: tag patch, GitHub Release
6. Mergear PR → develop (backport)
```

---

## Workflows CI/CD por branch

| Branch pattern | Workflow | Jobs | Tempo estimado | Auto-PR |
|---|---|---|---|---|
| `feat/**` `fix/**` `chore/**` etc. | `ci-feature.yml` | lint → unit-tests | ~5 min | Não |
| `develop` | `ci-develop.yml` | lint → testes → security + sonar → build | ~20 min | Não |
| `release/**` | `ci-release.yml` | lint → testes → security + sonar → build → **auto-pr** | ~22 min | Sim (→ main) |
| `hotfix/**` | `ci-hotfix.yml` | lint → testes → security → build → **auto-pr x2** | ~17 min | Sim (→ main + develop) |
| `main` `micro` | `ci.yml` (legado) | pipeline completo | ~20 min | Não |
| `main` (após CI) | `release.yml` | semver → build → GitHub Release | ~10 min | — |

---

## Configuração de proteção de branches (GitHub Settings)

Configurar em `Settings → Branches → Branch protection rules`:

### `main`
- [x] Require a pull request before merging
- [x] Require approvals: **1**
- [x] Require status checks to pass: `Lint & Análise Estática`, `Testes Unitários`
- [x] Require branches to be up to date before merging
- [x] Restrict who can push: ninguém (só via PR)
- [x] Do not allow bypassing the above settings

### `develop`
- [x] Require a pull request before merging
- [x] Require approvals: **1**
- [x] Require status checks to pass: `Lint & Análise Estática`, `Testes Unitários`
- [x] Require branches to be up to date before merging

---

## Convenção de nomenclatura

| Tipo | Padrão | Exemplo |
|---|---|---|
| Feature | `feat/descricao-kebab` | `feat/modulo-relatorios` |
| Fix | `fix/descricao-kebab` | `fix/erro-calculo-orcamento` |
| Hotfix | `hotfix/vX.Y.Z` ou `hotfix/descricao` | `hotfix/v1.2.1` |
| Release | `release/vX.Y.Z` | `release/v1.3.0` |
| Refactor | `refactor/descricao` | `refactor/service-pedido` |
| Chore | `chore/descricao` | `chore/atualiza-deps-spring` |

---

## Secrets e variáveis necessários no GitHub

| Nome | Tipo | Onde configurar | Usado por |
|---|---|---|---|
| `SONAR_TOKEN` | Secret | repo secrets | ci-develop, ci-release |
| `SONAR_PROJECT_KEY` | Variable | repo vars | ci-develop, ci-release |
| `SONAR_ORGANIZATION` | Variable | repo vars | ci-develop, ci-release |
| `GITHUB_TOKEN` | Secret (auto) | automático | release, auto-pr jobs |

---

## Diagrama de fluxo completo

```
feat/* ──push──► ci-feature (5m) ──PR──► develop
                                             │
                                        ci-develop (20m)
                                             │
                                       release/v* ──push──► ci-release (22m)
                                                                 │
                                                            [auto-PR → main]
                                                                 │
                                                            ci-release (PR)
                                                                 │
                                                         merge PR → main
                                                                 │
                                                          release.yml
                                                         (tag + GH Release)

main ──hotfix/*──► ci-hotfix (17m) ──┬──► [auto-PR → main]
                                     └──► [auto-PR → develop]
```
