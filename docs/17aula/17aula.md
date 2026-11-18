# Aula 17 — Auditoria e proteção da autenticação

## Objetivo

Acrescentar ao AL Sports uma defesa persistente contra tentativas on-line de
adivinhar senhas e uma trilha estruturada dos eventos de segurança relevantes.
A aula retoma o bloqueio pessimista da Aula 11, o `Clock` controlado da Aula 16,
o `AuthenticationManager` da Aula 12 e a autorização por perfis da Aula 14, sem
substituir nenhum desses mecanismos.

Este documento registra o que foi **efetivamente implementado e verificado** no
worktree `alsports-aula17`, branch `aula-17`, a partir do marco imutável da Aula
16 (`d055813c04a7fd18ac646cf62b331460d98a403e`, tag `aula-16-refresh-token`).

---

## O que esta aula não implementa

Esta aula é uma camada de *throttling* persistente por identificador de login e
uma trilha de eventos. Ela **não** implementa e não deve ser descrita como:

- WAF, CAPTCHA, MFA ou autenticação adaptativa;
- SIEM, correlação comportamental, reputação de IP ou análise de risco;
- rate limiter distribuído de borda ou proteção de rede;
- criptografia de trilha, assinatura de eventos ou não repúdio criptográfico.

A trilha facilita investigação e diagnóstico; ela não prova, por meios
criptográficos, quem executou uma ação. A resposta HTTP permanece genérica e o
bloqueio é temporário — nunca uma decisão administrativa permanente.

---

## Modelo de ameaça

| Ameaça | Descrição | Controle desta aula |
|---|---|---|
| Força bruta on-line | muitas senhas contra um mesmo login | contador por hash do login, janela e bloqueio temporário |
| Credential stuffing | credenciais vazadas reutilizadas em vários logins | contador por login e trilha de falhas; a correlação entre logins fica como risco residual |
| Password spraying | uma senha contra muitos logins | eventos e contador por login; **não** detecta padrões entre contas |
| Negação de serviço por lockout | atacante provoca bloqueio de vítima | bloqueio curto e configurável, sem bloqueio administrativo |
| Esvaziamento de armazenamento | logins aleatórios criam linhas e eventos | `CHECK` de formato do hash, índice único e necessidade de retenção em produção |
| Injeção em logs | CR/LF ou mensagem livre controlada pelo cliente | origem limitada a 45 caracteres sem CR/LF e enum fechado de tipos |

O bloqueio automático desta aula é distinto de `StatusContaAcesso.BLOQUEADA`.
Este último é uma decisão administrativa persistida em `conta_acesso.status`;
o primeiro é um estado transitório em `protecao_autenticacao.bloqueada_ate`.

---

## Política temporal e justificativa

Os valores didáticos são configuráveis:

| Propriedade | Padrão | Papel |
|---|---:|---|
| `maxFailedAttempts` | `5` | falhas na janela que disparam o bloqueio |
| `failureWindow` | `PT15M` | janela deslizante reiniciada quando expira |
| `lockDuration` | `PT15M` | duração do bloqueio automático |

Cinco tentativas equilibram usabilidade e risco para um sistema acadêmico de
loja: permitem erro humano razoável sem dar many tentativas a um atacante.
Quinze minutos de janela limitam a persistência do contador sem exigir limpeza
constante; quinze minutos de bloqueio desencorajam tentativas repetidas e
reduzem o impacto de um lockout malicioso. Não há número universal: em produção,
os valores devem ser justificados pelo risco da operação e pelo volume legítimo
de logins.

`ProtecaoAutenticacaoProperties` é um `record` com
`@ConfigurationProperties(prefix = "app.security.authentication-protection")`.
O construtor rejeita limite menor que 1, janela nula/zero/negativa e bloqueio
nulo/zero/negativo, impedindo que a aplicação suba com política indefinida:

```properties
app.security.authentication-protection.max-failed-attempts=${AUTHENTICATION_MAX_FAILED_ATTEMPTS:5}
app.security.authentication-protection.failure-window=${AUTHENTICATION_FAILURE_WINDOW:PT15M}
app.security.authentication-protection.lock-duration=${AUTHENTICATION_LOCK_DURATION:PT15M}
```

As três variáveis de ambiente são apenas limites e durações, não segredos, e
foram acrescentadas ao `.env.example` com valores padrão. O `.env` não foi lido,
exibido, copiado nem alterado.

---

## Identificador correlacionável e minimização

O texto do login é normalizado (`trim` + `toLowerCase(Locale.ROOT)`) e
transformado em **SHA-256 hexadecimal de 64 caracteres** minúsculos. Esse hash
é a única chave de correlação gravada em `protecao_autenticacao.chave_login` e,
quando aplicável, em `evento_seguranca.identificador_hash`.

O login em texto aberto, a senha, o hash BCrypt, o access token, o refresh
token, o hash do refresh token, o cabeçalho `Authorization` e corpos completos
de requisição **nunca** aparecem em tabelas de auditoria, eventos, logs da
aplicação, mensagens de erro ou documentação. `EventoSeguranca.toString()`
inclui apenas tipo, instante, IDs de conta e origem — nunca o hash.

---

## Domínio da proteção

`ProtecaoAutenticacao` guarda `chaveLogin`, `falhasConsecutivas`,
`janelaIniciadaEm` e `bloqueadaAte`:

- `registrarFalha` reinicia a contagem quando a janela anterior expirou,
  incrementa o contador e devolve `true` apenas na falha que ativa o bloqueio;
- `estaBloqueada` usa `agora.isBefore(bloqueadaAte)`, de modo que no instante
  exato do fim da janela o login volta a ser permitido;
- `registrarSucesso` zera contador, janela e bloqueio;
- o construtor exige hash `[0-9a-f]{64}`.

`EventoSeguranca` é imutável após a criação. Seu tipo é um enum fechado; IDs de
conta são valores históricos sem FK, para que a trilha sobreviva ao ciclo de
vida da conta. A origem aceita no máximo 45 caracteres (cobre IPv6 completo)
e rejeita CR/LF, evitando injeção de linhas em logs textuais. O hash é opcional
e, quando presente, precisa seguir o mesmo formato de 64 hexadecimais.

---

## Upsert, lock e concorrência

O primeiro registro de um hash inexistente é feito com upsert do PostgreSQL
(`INSERT ... ON CONFLICT DO NOTHING`). A atualização seguinte usa
`PESSIMISTIC_WRITE` sobre a linha do hash, serializando falhas concorrentes para
a mesma conta: duas transações que partem de três falhas terminam em **cinco**,
não quatro, e apenas uma delas emite `BLOQUEIO_TEMPORARIO_ATIVADO`.

A_atividade autônoma_ correspondente está em
`Aula17ProtecaoAuditoriaTest.duasFalhasConcorrentesNaoDevemPerderAtualizacaoDoContador`:
duas transações coordenadas por `CountDownLatch`, uma falha em cada, sem
`Thread.sleep`, com relógio controlado e limpeza explícita via JDBC.

---

## Transações e resposta genérica

O fluxo HTTP de login (`SessaoAutenticacaoService`) e o caso de uso legado
(`AutenticacaoService`) compartilham a mesma política:

1. `permitirTentativa` consulta o hash; se houver bloqueio ativo, registra
   `LOGIN_RECUSADO_BLOQUEIO` e lança a exceção de credenciais inválidas **sem
   comparar a senha**;
2. falha de autenticação registra `LOGIN_FALHA` (e, se aplicável,
   `BLOQUEIO_TEMPORARIO_ATIVADO`) antes de lançar a exceção;
3. sucesso registra `LOGIN_SUCESSO` e limpa o contador.

Falhas e recusas usam `@Transactional(propagation = REQUIRES_NEW)`: a resposta
HTTP é 401 e a transação do caso de uso faria rollback, apagando contador e
evento. A transação própria garante persistência antes da resposta. Sucesso e
operações administrativas participam da transação do chamador: se a alteração
reverter, o evento também reverte.

A resposta externa é sempre a mesma para login inexistente, senha incorreta,
conta administrativamente bloqueada, pessoa inativa e bloqueio temporário
ativo:

```json
{
  "timestamp": "2026-11-17T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Login ou senha inválidos",
  "path": "/api/autenticacao/login",
  "fields": null
}
```

Nenhum desses casos revela qual condição falhou nem quanto tempo dura o
bloqueio.

---

## Vocabulário de eventos

| Tipo | Quando é emitido |
|---|---|
| `LOGIN_SUCESSO` | credenciais válidas e proteção permitida |
| `LOGIN_FALHA` | login inexistente ou senha incorreta |
| `LOGIN_RECUSADO_BLOQUEIO` | tentativa recusada durante bloqueio temporário |
| `BLOQUEIO_TEMPORARIO_ATIVADO` | quinta falha na janela, uma vez por ativação |
| `CONTA_CRIADA` | administração cria conta de acesso |
| `SENHA_ALTERADA` | troca da própria senha |
| `PERFIS_ALTERADOS` | administração substitui perfis |
| `STATUS_ALTERADO` | administração ativa ou bloqueia conta |
| `REFRESH_SUCESSO` | renovação válida do par de tokens |
| `REFRESH_FALHA` | renovação recusada |
| `LOGOUT` | encerramento local da família de sessão |
| `LOGOUT_GLOBAL` | encerramento de todas as sessões da conta |

O tipo é enum no domínio e `CHECK` no banco: não existe mensagem livre
controlada pelo cliente. A auditoria administrativa usa o login do
administrador autenticado para resolver o ID do ator; quando o login não pode
ser correlacionado, o campo permanece nulo em vez de registrar texto em claro.

`Aula17AuditoriaObrigatoriaTest` demonstra que a trilha não é decorativa: ao
forçar falha na gravação de `STATUS_ALTERADO`, a conta permanece `ATIVA` e
nenhum evento é confirmado; ao restaurar a gravação, status e evento são
confirmados juntos.

---

## Consulta administrativa

`GET /api/auditoria-seguranca?page=0&size=20&tipo=LOGIN_FALHA`

- `page` começa em zero; `size` vai de 1 a 100; valores fora desses limites
  retornam `400`;
- `tipo` é opcional e aceita somente os valores do enum, sem diferenciar caixa;
- ordenação estável: `ocorrido_em DESC, id DESC`;
- resposta paginada no formato Spring `Page` (`content`, `totalElements`,
  `number`, `size`, `totalPages`), sem `identificadorHash`, senha, tokens ou
  mensagem livre;
- somente leitura: não há endpoint de escrita, alteração ou exclusão;
- `ADMINISTRADOR` é exigido duas vezes: na cadeia HTTP e por
  `@PreAuthorize("hasRole('ADMINISTRADOR')")` no service. OPERADOR recebe
  `403`; sem token, `401`.

O contrato OpenAPI documenta o endpoint, o parâmetro `tipo`, os códigos
200/400/401/403, `bearerAuth` e o schema `EventoSegurancaResponse`, com
asserções explícitas de ausência de `identificadorHash` e segredos.

---

## Migration 010

`src/main/resources/db/changelog/changes/010-auditoria-protecao-autenticacao.yaml`,
incluída no changelog mestre, contém três changeSets:

1. `010-01-create-protecao-autenticacao` — tabela com `chave_login VARCHAR(64)`
   única, `falhas_consecutivas >= 0` e `CHECK` de hash hexadecimal;
2. `010-02-create-evento-seguranca` — tabela com `CHECK` do enum de 12 tipos,
   IDs positivos, formato do hash e origem sem CR/LF;
3. `010-03-indices-auditoria-protecao` — índice por `bloqueada_ate`, índice
   composto por (`ocorrido_em`, `id`) e índice por (`tipo`, `ocorrido_em`,
   `id`).

Cada changeSet possui rollback explícito na ordem inversa. A edição comunitária
do Liquibase não aceita `addCheckConstraint` nesse contexto, por isso as
constraints foram criadas com SQL e removidas com `DROP CONSTRAINT IF EXISTS`.

O histórico real do AL Sports possuía **43** changeSets antes da aula; com a
010 passou a **46**. O número citado no material didático de origem pertence a
outro histórico e não foi forçado artificialmente. A complementação publicada
das correções V1–V4 acrescentou a migration
`011-adicionar-geracao-sessao-conta-acesso.yaml`, com o changeset
`011-01-adicionar-geracao-sessao-conta-acesso`, elevando o estado atual a
**47** changeSets.

`Aula17MigrationRollbackTest` cria um banco PostgreSQL temporário exclusivo,
aplica os 47 changeSets do estado atual (46 no marco original da aula), verifica
tabelas/constraints/índices, confirma a rejeição de hash inválido, contador
negativo, tipo livre e CR/LF, reverte os 3 changeSets da 010 e confirma que
`010-%` desaparece do `databasechangelog`. Depois reaplica e remove o banco com
`with (force)`. O banco compartilhado `alsports_test` não é apagado nem
reinicializado.

---

## Complementação publicada (correções V1–V4)

Após o marco original desta aula, as correções V1–V4 foram publicadas na
própria branch `aula-17`, sem alterar a explicação pedagógica apresentada
acima:

- commit `b6043c5386b290dc12bbb9c51360bcb8fd282f02` —
  `fix: corrige concorrencia e protecao da autenticacao`;
- tag `aula-17-correcao-seguranca-v1-v4`;
- migration `011-adicionar-geracao-sessao-conta-acesso.yaml`
  (`011-01-adicionar-geracao-sessao-conta-acesso`);
- histórico atual: 47 changeSets (46 no marco original da Aula 17).

Esta seção registra o estado publicado. As seções anteriores continuam
descrevendo a Aula 17 como ela foi efetivamente conduzida: a correção de
concorrência e a geração de sessão não faziam parte do plano original da aula.
A sequência pedagógica oficial, a evolução realizada no projeto e o estado
atual são camadas distintas; as divergências históricas das Aulas 00, 01, 03,
04 e 05 permanecem preservadas, sem reescrita de commits, tags ou datas.

---

## Testes

| Classe | Abrangência |
|---|---|
| `ProtecaoAutenticacaoTest` | fronteiras de janela, bloqueio, limpeza no sucesso, validação do hash |
| `EventoSegurancaTest` | enum, IDs, hash, origem sem CR/LF e `toString` sem segredos |
| `ProtecaoAutenticacaoPropertiesTest` | rejeição de limite, janela e duração inválidos |
| `Aula17ProtecaoAuditoriaTest` | HTTP real, resposta genérica, bloqueio temporal, administração auditada, refresh/logout, consulta protegida, concorrência |
| `Aula17MigrationRollbackTest` | aplicação/rollback isolado em PostgreSQL temporário |
| `Aula17AuditoriaObrigatoriaTest` | falha de auditoria reverte alteração sensível |
| `OpenApiDocumentationTest` | extensão do contrato com o endpoint e o DTO da aula |

A suíte anterior tinha 398 testes aprovados; a suíte final da Aula 17 tem
**425 testes aprovados**, com 0 falhas, 0 erros e 0 ignorados em `mvn -B test`
e `mvn -B clean verify`. Nenhum teste foi removido nem desabilitado; os testes
que não avaliam segurança continuam com filtros desabilitados apenas no próprio
caso de uso, e os testes de segurança mantêm a cadeia do Spring Security ativa.
O `jacoco:check` aprovou 87,03% de linhas e 75,07% de branches (116 classes
analisadas), acima do portão de 70% / 40%.

---

## Consultas SQL de estudo

```sql
SELECT id, chave_login, falhas_consecutivas, janela_iniciada_em, bloqueada_ate
FROM protecao_autenticacao
ORDER BY id;
```

```sql
SELECT id, tipo, ocorrido_em, ator_conta_id, alvo_conta_id, endereco_origem
FROM evento_seguranca
ORDER BY ocorrido_em DESC, id DESC
LIMIT 50;
```

```sql
SELECT endereco_origem, COUNT(*) AS quantidade
FROM evento_seguranca
WHERE tipo = 'LOGIN_FALHA'
GROUP BY endereco_origem
ORDER BY quantidade DESC;
```

Não projete `identificador_hash` em telas ou relatórios sem necessidade
definida. A origem é dado pessoal em potencial e seu acesso deve ser
justificado.

---

## Postman

A collection
`docs/postman/AL-Sports-Aula-17.postman_collection.json` preserva as pastas
anteriores e acrescenta **`11 — Proteção e auditoria da autenticação`**, com
sete requisições:

1. login com identificador desconhecido e verificação de `401`/mensagem genérica;
2. falha de senha — enviar cinco vezes com uma conta descartável;
3. senha correta durante o bloqueio, que continua `401`;
4. consulta administrativa filtrada por `LOGIN_FALHA`;
5. consulta administrativa filtrada por `BLOQUEIO_TEMPORARIO_ATIVADO`;
6. operador tentando consultar a auditoria — `403`;
7. consulta sem token — `401`.

Os scripts verificam ausência de `identificadorHash`, senha, access token,
refresh token e mensagem livre na resposta. `loginInexistente` e `senhaErrada`
são valores fictícios; `contaDescartavelLogin` e `contaDescartavelSenha` devem
ser preenchidos localmente. Nenhuma credencial real ou token válido está na
collection. O bloqueio é persistido no banco de desenvolvimento: não faça o
experimento com a conta OPERADOR usada pelas outras pastas.

---

## Ameaças, controles e evidências

| Ameaça | Controle implementado | Evidência de teste | Risco residual |
|---|---|---|---|
| Força bruta contra um login | 5 falhas em 15 min → bloqueio de 15 min com hash SHA-256 | `ProtecaoAutenticacaoTest`, `Aula17ProtecaoAuditoriaTest` | janela e limite podem precisar de ajuste por domínio |
| Atualização perdida entre falhas simultâneas | upsert + `PESSIMISTIC_WRITE` na linha do hash | teste concorrente com duas transações coordenadas | contendor de banco participa de toda falha |
| Contador apagado pelo HTTP 401 | `REQUIRES_NEW` em falha/recusa | verificação do banco após resposta 401 | gravação síncrona acrescenta latência ao login |
| Enumeração de contas | resposta idêntica para todos os motivos de recusa | asserções de corpo e status nos testes HTTP | tempo de resposta ainda pode variar; não é medido nesta aula |
| Vazamento em logs/trilha | sem mensagem livre, hash validado, origem sem CR/LF, DTO restrito | `EventoSegurancaTest`, asserções do DTO/OpenAPI | logs de infraestrutura fora da aplicação exigem configuração própria |
| Alteração administrativa sem registro | evento na mesma transação da alteração | `Aula17AuditoriaObrigatoriaTest` | auditoria não é assinada, não é não repúdio |
| Consulta da trilha por perfil incorreto | `hasRole` na HTTP e `@PreAuthorize` no service | 403 para OPERADOR nos dois níveis | administrador autorizado pode ler origem; acesso deve ser governado |
| Injeção por CR/LF ou tipo livre | `CHECK` no banco e validação no domínio | `Aula17MigrationRollbackTest` | outras colunas fora desta migration não são avaliadas aqui |

---

## Riscos residuais

1. **Password spraying**: uma senha contra muitos logins mantém cada contador
   abaixo do limite. A trilha registra as falhas, mas não correlaciona contas.
2. **Ataque distribuído**: muitos endereços contra a mesma conta ainda são
   contados por login; muitos logins e muitas origens exigem correlação e controle
   de borda, fora do escopo desta aula.
3. **Preenchimento da tabela**: logins inexistentes geram linha e evento.
   Produção precisa de limite global, retenção, limpeza de contadores antigos,
   quotas e monitoramento.
4. **Múltiplas instâncias**: o estado no PostgreSQL é compartilhado entre
   instâncias, o que é melhor que um `Map` local, mas coloca o banco no
   caminho crítico das falhas.
5. **IP atrás de proxy**: sem configuração confiável, a aplicação registra o
   proxy. Confiar em cabeçalho enviado pelo cliente criaria dado forjável.
6. **Retenção e privacidade**: eventos não devem ser guardados indefinidamente
   apenas porque são úteis. Finalidade, acesso, prazo, descarte, backup e
   resposta a incidentes precisam ser definidos pela organização.
7. **Lockout como negação de serviço**: o bloqueio é temporário e curto, mas
   ainda pode incomodar usuários legítimos. Monitore
   `BLOQUEIO_TEMPORARIO_ATIVADO` e avalie atraso progressivo ou análise de
   risco antes de adotar limites mais agressivos.

---

## Comandos

```powershell
$u = [Environment]::GetEnvironmentVariable('DB_TEST_PASSWORD','User')
$env:DB_TEST_PASSWORD = $u
mvn -B test
mvn -B clean verify
```

A presença da variável foi confirmada apenas booleanamente e seu valor nunca
foi exibido. Os testes usam PostgreSQL real, `Clock` controlado e nenhum
`Thread.sleep`.

Para verificar a collection:

```powershell
node -e "JSON.parse(require('fs').readFileSync('docs/postman/AL-Sports-Aula-17.postman_collection.json','utf8').replace(/^\uFEFF/,''))"
git diff --check
```

---

## Checklist de verificação

- [x] configuração tipada rejeita valores inválidos;
- [x] login normalizado antes do SHA-256;
- [x] texto do login não aparece em evento de falha;
- [x] quinta falha ativa o bloqueio e a sexta tentativa é recusada;
- [x] bloqueio expira exatamente no prazo com relógio controlado;
- [x] sucesso limpa o contador;
- [x] conta inexistente usa a mesma resposta 401;
- [x] atualizações concorrentes usam lock e terminam em cinco;
- [x] eventos não contêm senhas, hashes BCrypt, tokens ou cabeçalhos;
- [x] alterações administrativas são auditadas e revertem com falha da trilha;
- [x] refresh e logout são auditados;
- [x] operador recebe 403 na consulta e administrador pagina e filtra;
- [x] migration 010 aplica, reverte e reaplica em banco isolado;
- [x] 47 changeSets no banco real de teste no estado atual (46 no marco original da aula);
- [x] Postman é JSON válido e contém a pasta da Aula 17;
- [x] contrato OpenAPI descreve o endpoint sem expor segredos;
- [x] cobertura permanece acima do portão da Aula 15 (70% linhas / 40% branches).
