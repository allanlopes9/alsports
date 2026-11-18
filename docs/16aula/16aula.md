# Aula 16 — Ciclo de vida dos tokens: renovação, inatividade, rotação e revogação

## Objetivo

Implementar sessões renováveis no AL Sports sem transformar o access token em
credencial permanente. O access token continua sendo um JWT HS256 *stateless*;
o refresh token é opaco, de uso único, armazenado apenas como hash e organizado
em famílias. A aula cobre emissão, rotação, detecção de reutilização,
expiração por inatividade e absoluta, logout local/global, concorrência real,
testes, OpenAPI, Postman e documentação.

Este documento registra o que foi **efetivamente implementado e medido** no
worktree `alsports-aula16`, branch `aula-16`, a partir do commit base
`6b06ab3` (Aula 15).

---

## Access token versus refresh token

| Aspecto | Access token | Refresh token |
|---|---|---|
| Formato | JWT assinado HS256 | Sequência opaca Base64URL |
| Conteúdo | `iss`, `sub`, `iat`, `exp`, `jti`, `perfis`, `sid` | Nenhum dado legível |
| Validade | 30 minutos (`PT30M`) | 30 min de inatividade / 8 h absolutos |
| Armazenamento no servidor | Nenhum | somente hash SHA-256 da geração atual e anteriores |
| Uso | `Authorization: Bearer` nas rotas de negócio | corpo de `/refresh` e `/logout` |
| Renovação | não se renova; expira no `exp` | gera novo par na mesma família |

O Resource Server **não consulta** a tabela de sessões a cada requisição. O
`logout` impede novas renovações, mas um access token já emitido continua válido
até seu próprio `exp`. Não há denylist nem introspecção nesta aula.

---

## Os três relógios

1. **Access token**: `iat`/`exp`, absoluto de 30 minutos.
2. **Inatividade do refresh**: 30 minutos desde `ultimaAtividadeEm`.
3. **Limite absoluto da família**: 8 horas desde `criadaEm`, herdado por todas
   as rotações e jamais deslocado.

A expiração ocorre quando o instante atual **atinge ou ultrapassa** o limite.
O limite absoluto é verificado antes da inatividade para que o motivo auditável
seja `EXPIRACAO_ABSOLUTA` quando ambas as condições coincidirem.

`Clock` é injetado na lógica de produção (`JwtConfig` e
`SessaoAutenticacaoService`). Os testes usam relógio controlado e não usam
`sleep`.

---

## Geração e armazenamento seguro

- `SecureRandom` gera **32 bytes** (256 bits) por refresh token.
- Codificação: **Base64URL sem padding**.
- Persistência: **SHA-256 em hexadecimal de 64 caracteres** no campo
  `token_hash`.
- O valor opaco existe apenas no DTO de resposta e no corpo da requisição
  seguinte. Não é gravado em banco, entidade, log, mensagem de erro,
  documentação nem arquivo.
- Senhas continuam usando BCrypt — hash de senha e hash de refresh token são
  mecanismos distintos.

---

## Família, rotação e reutilização

No login, uma família é identificada por `UUID.randomUUID()`. Cada renovação:

1. localiza a sessão pelo hash com `PESSIMISTIC_WRITE`;
2. verifica revogação, uso prévio, expiração absoluta, inatividade e situação
   da conta/pessoa;
3. marca a geração atual como utilizada;
4. gera novo refresh token e persiste apenas seu hash;
5. mantém `familiaId` e o `expiraEm` originais;
6. emite novo access token com o mesmo `sid`.

Se um token já utilizado for reapresentado, **todas** as gerações ativas da
família são revogadas com motivo `REUTILIZACAO`, inclusive o sucessor válido
mais recente. A resposta é HTTP 401 genérica.

`@Transactional(noRollbackFor = RefreshTokenInvalidoException.class)` garante
que a revogação seja confirmada no PostgreSQL mesmo quando a resposta HTTP é
401. O teste `c10_revogacaoDevePersistirMesmoComResposta401` verifica o banco,
não apenas o status.

---

## Modelo de domínio

- `SessaoRefreshToken` — entidade JPA com `id`, `conta` (LAZY), `familiaId`,
  `tokenHash`, `criadaEm`, `ultimaAtividadeEm`, `expiraEm`, `utilizadaEm`,
  `revogadaEm`, `motivoRevogacao`.
- `MotivoRevogacaoRefreshToken` — `LOGOUT`, `LOGOUT_GLOBAL`, `INATIVIDADE`,
  `EXPIRACAO_ABSOLUTA`, `REUTILIZACAO`, `CONTA_INATIVA` (persistido como texto).
- Métodos de domínio: `nova`, `marcarUtilizada`, `revogar`,
  `expirouPorInatividade`, `expirouPorLimiteAbsoluto`, `podeRenovar`.
- Transições inválidas (reutilizar, revogar duas vezes, usar após expiração)
  lançam `IllegalStateException`.

Gerações já utilizadas **não são apagadas**: elas permitem detectar
reutilização posterior.

---

## Migration

`009-sessoes-refresh-token.yaml`, incluída no changelog mestre após
`008-contas-acesso.yaml`, com três changeSets:

1. criação de `sessao_refresh_token` com `TIMESTAMP WITH TIME ZONE`;
2. FK para `conta_acesso` (CASCADE) e índices por família, conta e hash;
3. unicidade de `token_hash`, CHECK `expira_em > criada_em` e CHECK dos valores
   permitidos de `motivo_revogacao`.

A baseline possuía 40 changeSets; a migration adiciona 3, totalizando **43**.
O material oficial cita 36 changeSets em outro histórico de migrations; o
número real do AL Sports prevalece, sem forçar valores artificiais.

---

## Configurações tipadas

`RefreshTokenProperties`, prefixo `app.security.refresh-token`:

```properties
app.security.refresh-token.idle-timeout=${REFRESH_TOKEN_IDLE_TIMEOUT:PT30M}
app.security.refresh-token.absolute-expiration=${REFRESH_TOKEN_ABSOLUTE_EXPIRATION:PT8H}
```

Validações: `idleTimeout` positivo; `absoluteExpiration` positivo e maior que
`idleTimeout`. As duas variáveis de ambiente foram acrescentadas ao
`.env.example` como valores **não secretos**. O `.env` original não foi lido nem
alterado.

---

## Locking, transações e concorrência

`SessaoRefreshTokenRepository.buscarPorHashParaAtualizacao` usa
`@Lock(PESSIMISTIC_WRITE)` e `join fetch` de `conta` e `pessoa`, evitando
`LazyInitializationException` na validação. O teste
`CicloTokenConcorrenciaTest` usa PostgreSQL real, duas requisições MockMvc
simultâneas e nenhum mock: o resultado observado é **uma renovação 200 e uma
401**, com dois registros na família e exatamente uma geração marcada como
utilizada.

---

## Claim `sid`

`TokenService.emitir(Authentication, String sid)` preserva a sobrecarga
original e acrescenta a claim `sid` com o identificador da família quando
informada. HS256, issuer, `exp`, `perfis`, autorização por roles e o conversor
da Aula 14 permanecem inalterados.

---

## Contratos HTTP

| Endpoint | Corpo | Resposta |
|---|---|---|
| `POST /api/autenticacao/login` | `{login, senha}` | 200 com par de tokens |
| `POST /api/autenticacao/refresh` | `{refreshToken}` | 200 com novo par, 401 se inválido |
| `POST /api/autenticacao/logout` | `{refreshToken}` | 204 sempre, sem revelar estado do token |
| `POST /api/autenticacao/logout-todos` | sem corpo, exige Bearer | 204 |

`TokenResponse` expõe `accessToken`, `tokenType`, `expiresIn`, `expiresAt`,
`refreshToken` e `refreshTokenExpiresAt`. Hash, `familiaId` e dados internos
não são serializados.

Erro de refresh: HTTP 401, mensagem exata `Refresh token inválido ou expirado`,
mesmo para token ausente, desconhecido, reutilizado, revogado, expirado ou de
conta inativa. `SecurityConfig` mantém login/refresh/logout públicos, exige
Bearer no logout global e preserva `denyAll` e as regras de perfis anteriores.

---

## Decisão arquitetural do AL Sports

1. **Access token:** manter em memória no cliente; não persistir em
   `localStorage` quando houver cliente web. O projeto ainda não possui cliente
   próprio — esta é a recomendação, não uma implementação existente.
2. **Refresh token:** também em memória, idealmente em *service worker* ou
   canal HTTP-only seguro quando o cliente existir. Decisão final pendente
   porque não há aplicação cliente neste repositório.
3. **Durações:** 30 min de access e inatividade equilibram usabilidade e
   exposição; 8 h de limite absoluto forçam reautenticação diária.
4. **Access token após logout:** continua válido até `exp`; logout não revoga
   JWT já emitido.
5. **Ameaça reduzida pela rotação:** roubo de um refresh token antigo é
   detectado na reapresentação e provoca revogação da família inteira.
6. **Renovações simultâneas:** `PESSIMISTIC_WRITE` seraliza o uso da geração;
   uma vence e a outra recebe 401, sem dois sucessores válidos.

---

## Testes

- `SessaoRefreshTokenTest` — 16 testes de domínio, sem Spring/banco, com
  fronteiras temporais exatas.
- `RefreshTokenPropertiesTest` — 3 testes de validação das durações.
- `CicloTokenApiTest` — 23 testes HTTP com PostgreSQL real e relógio
  controlado, cobrindo os 23 cenários do prompt e a atividade da conta
  bloqueada após emissão.
- `CicloTokenConcorrenciaTest` — 1 teste de concorrência real sem
  `@Transactional` de classe, com limpeza explícita via JDBC.
- Baseline: **353** testes. Final: **396** testes, zero falhas, zero erros,
  zero ignorados.

---

## OpenAPI e Postman

O contrato OpenAPI documenta login, refresh, logout, logout-todos, DTOs
`LoginRequest`, `RefreshTokenRequest`, `TokenResponse`, códigos 200/204/400/401,
mensagem genérica e o esquema Bearer apenas no logout global. O teste
`OpenApiDocumentationTest` verifica esses elementos.

A coleção `docs/postman/AL-Sports-Aula-17.postman_collection.json` preserva as
pastas anteriores e acrescenta a pasta de autorização e ciclo de tokens, hoje
numerada `10 - Autorização e ciclo de tokens` (o nome original era
`02 - Autorização e ciclo de tokens`; a numeração foi ajustada para eliminar a
duplicidade com `02 - Fornecedores`), com 12
requests: login admin, captura do par, criação de operador, login do operador,
rota autorizada, 403 administrativa, renovação, substituição dos tokens,
logout local, 401 na renovação seguinte, uso do access token até `exp` e
logout global. Nenhuma senha pessoal ou token real está na coleção.

---

## Ameaças e riscos residuais

- Access token emitido continua válido até `exp` após logout (decisão
  consciente, sem denylist).
- O cliente ainda não existe; o armazenamento seguro do refresh token depende
  de decisão futura.
- Logs de acesso e proxies reversos devem ser configurados para não registrar
  corpos de `/login`, `/refresh` e `/logout`.
- A detective de reutilização revoga a família de forma conservadora: um
  cliente legítimo que reapresenta token antigo também perde a sessão.
- `logout` local com token desconhecido retorna 204 para não permitir
  enumeração; isso impede o cliente de distinguir sessão já encerrada.

---

## Comandos

```powershell
$u = [Environment]::GetEnvironmentVariable('DB_TEST_PASSWORD','User')
$env:DB_TEST_PASSWORD = $u
mvn test
mvn clean verify
```

O processo Maven precisa receber `DB_TEST_PASSWORD`; a presença foi
confirmada apenas booleanamente e o segredo nunca foi exibido.

## Resultados efetivamente obtidos

- `mvn test`: 396 testes, 0 falhas, 0 erros, BUILD SUCCESS.
- `mvn clean verify`: BUILD SUCCESS e portão JaCoCo aprovado (70% linhas,
  40% branches, limites inalterados).
- Cobertura e changeSets finais são registrados no relatório de entrega da aula.
