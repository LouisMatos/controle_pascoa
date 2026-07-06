## Tipo de mudança

<!-- Marque o tipo desta PR -->
- [ ] `feat` — Nova funcionalidade
- [ ] `fix` — Correção de bug
- [ ] `hotfix` — Correção crítica em produção
- [ ] `release` — Release candidate
- [ ] `refactor` — Refatoração sem mudança de comportamento
- [ ] `perf` — Melhoria de performance
- [ ] `chore` — Manutenção (deps, config, CI)
- [ ] `docs` — Documentação

## Descrição

<!-- O que foi feito e por quê (não o como — o código já diz o como) -->

## Mudanças no banco de dados

- [ ] Esta PR **não** inclui migrations Flyway
- [ ] Inclui migration `V{N}__{descricao}.sql` — testada localmente

## Checklist

- [ ] Testes unitários adicionados/atualizados
- [ ] `docs/05-estado-implementacao.md` atualizado (se novo módulo ou bug resolvido)
- [ ] Novas rotas adicionadas em `SecurityConfig.java`
- [ ] Sem `@Autowired` — injeção via `@RequiredArgsConstructor`
- [ ] Sem `ddl-auto=create/update` — apenas migrations Flyway

## Testes realizados

<!-- Descrever o que foi testado manualmente -->

## Screenshots (se UI)

<!-- Remover esta seção se não há mudança de UI -->
