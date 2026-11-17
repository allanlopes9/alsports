# Aula 12 — Autenticação, proteção de senhas e Spring Security

[⬅ Voltar para o índice do curso](../../README.md)

---

## Apresentação

Até a Aula 11, a API possuía regras de negócio, transações e controle de concorrência, mas qualquer cliente que conhecesse o endereço podia consultar ou alterar os dados. Faltava responder a uma pergunta essencial: **quem está fazendo esta requisição?**

Nesta aula o AL Sports passa a ter uma identidade de acesso vinculada a `Pessoa`, armazena a senha somente como hash BCrypt e protege a API com Spring Security. O mecanismo escolhido é o HTTP Basic porque ele torna o ciclo de autenticação visível com poucos componentes. JWT, OAuth2 e login social não fazem parte desta etapa.

> **Problema orientador:** como confirmar a identidade de quem chama a API sem armazenar ou devolver sua senha?

## Objetivos

Ao final da aula, o projeto deve:

- diferenciar identificação, autenticação e autorização;
- separar função profissional de perfil de acesso;
- explicar hash, salt e fator de trabalho;
- criar a entidade `ContaAcesso` e seus perfis;
- versionar as tabelas com Liquibase;
- integrar o banco ao `UserDetailsService`;
- persistir senhas somente com BCrypt;
- proteger as rotas com HTTP Basic;
- documentar o esquema de segurança no OpenAPI;
- testar autenticação, autorização e não exposição de credenciais.

## Identificação, autenticação e autorização

Os três conceitos são diferentes:

- **identificação**: quem o cliente afirma ser, por exemplo o login informado;
- **autenticação**: prova de que a identidade é verdadeira, feita pela conferência da senha;
- **autorização**: decisão sobre o que a identidade autenticada pode fazer.

Nesta aula a autenticação é implementada. A autorização é mantida simples: todas as rotas protegidas exigem uma conta autenticada, sem regras diferenciadas por perfil. Os perfis já são modelados e convertidos em autoridades para permitir evolução futura.

## Separação entre pessoa, papel, função e conta

O modelo do AL Sports preserva responsabilidades distintas:

- `Pessoa`: identidade civil, com nome, e-mail, CPF e status;
- `Cliente`: papel de negócio de quem compra;
- `Colaborador`: papel de negócio de quem trabalha na loja;
- `FuncaoColaborador`: função profissional exercida pelo colaborador;
- `ContaAcesso`: credencial de autenticação;
- `PerfilAcesso`: perfil de autorização da conta.

Essa separação evita misturar conceitos:

- uma pessoa pode ser cliente ou colaboradora sem possuir conta;
- uma conta não representa uma função profissional;
- `FuncaoColaborador` não concede autorização;
- `PerfilAcesso` não descreve o trabalho exercido.

Nesta versão, cada pessoa possui no máximo uma conta de acesso. Uma conta nova é sempre criada com status `ATIVA` e somente com o perfil `OPERADOR`. O cadastro público não permite escolher perfil.

## Hash, salt e fator de trabalho

Uma senha não deve ser armazenada em texto aberto. O projeto usa um **hash criptográfico** com BCrypt, que possui três propriedades importantes:

- **função de mão única**: não existe operação prática para recuperar a senha a partir do hash;
- **salt aleatório**: cada senha recebe um valor aleatório, de modo que senhas iguais produzam hashes diferentes;
- **fator de trabalho**: o custo do cálculo é ajustável, tornando ataques de força bruta mais caros.

O Spring Security fornece `PasswordEncoderFactories.createDelegatingPasswordEncoder()`. O formato delegado grava o identificador do algoritmo junto ao hash, por exemplo:

```text
{bcrypt}$2a$10$...
```

O prefixo `{bcrypt}` permite que o algoritmo seja identificado e evoluído no futuro. O hash completo tem cerca de 60 caracteres, além do prefixo, e é armazenado em `conta_acesso.senha_hash` com tamanho máximo de 255 caracteres.

A senha em texto aberto existe apenas durante o request. Ela nunca é:

- persistida no banco;
- devolvida em respostas;
- incluída em `toString()`;
- registrada em logs;
- incluída em mensagens de erro;
- documentada como campo de resposta.

## Limites de senha

A senha possui duas validações complementares:

- entre 12 e 72 caracteres;
- no máximo 72 bytes na codificação UTF-8.

O limite de 72 bytes é relevante porque o BCrypt considera apenas os primeiros 72 bytes. Sem essa validação, dois valores diferentes poderiam produzir o mesmo hash. Caracteres acentuados ocupam mais de um byte em UTF-8, por isso a verificação em bytes é feita além da contagem de caracteres.

O login aceita:

- entre 3 e 80 caracteres;
- letras, números, ponto, sublinhado e hífen;
- comparação sem diferenciar maiúsculas, minúsculas ou espaços externos.

## Modelo de domínio

### `StatusContaAcesso`

```java
public enum StatusContaAcesso {
    ATIVA,
    BLOQUEADA
}
```

### `PerfilAcesso`

```java
public enum PerfilAcesso {
    OPERADOR,
    ADMINISTRADOR
}
```

### `ContaAcesso`

A entidade JPA possui:

- `id`;
- `pessoa` — `@OneToOne` com carregamento `LAZY`;
- `login`;
- `senhaHash`;
- `status`;
- `perfis` — `@ElementCollection` com carregamento `EAGER`.

Regras principais:

- `podeAutenticar()` retorna verdadeiro somente quando a conta está `ATIVA` e a pessoa está ativa;
- `alterarSenha(String senhaHash)` substitui somente o hash;
- `bloquear()` e `ativar()` alteram o status;
- `concederPerfil(PerfilAcesso)` permite provisionamento administrativo futuro, mas não é exposto no cadastro público;
- `getPerfis()` devolve uma coleção imutável;
- `toString()` não inclui o hash.

## Migration

A migration da Aula 12 é independente e posterior à 007:

```text
src/main/resources/db/changelog/changes/008-contas-acesso.yaml
```

Ela é incluída em:

```text
src/main/resources/db/changelog/db.changelog-master.yaml
```

Changesets:

- `008-01-create-conta-acesso`;
- `008-02-create-conta-acesso-perfil`;
- `008-03-constraints-conta-acesso`;
- `008-04-constraints-conta-acesso-perfil`.

### Tabela `conta_acesso`

| Coluna | Tipo | Observação |
| --- | --- | --- |
| `id` | `BIGINT` | chave primária, gerada automaticamente |
| `pessoa_id` | `BIGINT` | obrigatório, único, FK para `pessoa.id` |
| `login` | `VARCHAR(80)` | obrigatório |
| `senha_hash` | `VARCHAR(255)` | obrigatório, somente hash |
| `status` | `VARCHAR(20)` | `ATIVA` ou `BLOQUEADA` |

Constraints relevantes:

- `uk_conta_acesso_pessoa`: uma conta por pessoa nesta versão;
- `fk_conta_acesso_pessoa`: vínculo com `pessoa`, com `ON DELETE RESTRICT`;
- `uk_conta_acesso_login_lower`: índice único sobre `LOWER(BTRIM(login))`;
- `ck_conta_acesso_status`: restringe o status;
- `ck_conta_acesso_login_tamanho`: login entre 3 e 80 caracteres após `BTRIM`.

### Tabela `conta_acesso_perfil`

| Coluna | Tipo | Observação |
| --- | --- | --- |
| `conta_acesso_id` | `BIGINT` | parte da PK composta, FK para `conta_acesso.id` |
| `perfil` | `VARCHAR(30)` | parte da PK composta, `OPERADOR` ou `ADMINISTRADOR` |

Constraints relevantes:

- `pk_conta_acesso_perfil`: chave primária composta;
- `fk_conta_acesso_perfil_conta`: vínculo com a conta, com `ON DELETE CASCADE`;
- `ck_conta_acesso_perfil_valor`: restringe os perfis conhecidos.

A migration não cria contas, senhas ou hashes reais. O rollback remove constraints e tabelas na ordem inversa, preservando a integridade referencial.

## Repository

`ContaAcessoRepository` oferece:

```java
boolean existsByLoginIgnoreCase(String login);
boolean existsByPessoaId(Long pessoaId);
Optional<ContaAcesso> findByLoginIgnoreCase(String login);
```

A consulta de autenticação usa:

```java
@EntityGraph(attributePaths = {"pessoa", "perfis"})
Optional<ContaAcesso> findByLoginIgnoreCase(String login);
```

O `@EntityGraph` carrega a pessoa e os perfis na mesma consulta, evitando `LazyInitializationException` durante a montagem do `UserDetails` e reduzindo consultas adicionais.

## DTOs

### `ContaAcessoRequest`

Campos:

- `pessoaId` — obrigatório;
- `login` — 3 a 80 caracteres, padrão `^[A-Za-z0-9._-]+$`;
- `senha` — 12 a 72 caracteres, documentada como `password` e somente escrita.

O DTO não possui campo de perfil. Um request que envie `perfis` é ignorado pelo contrato e a conta continua sendo criada somente com `OPERADOR`.

### `AlteracaoSenhaRequest`

Campos:

- `senhaAtual`;
- `novaSenha`.

### `ContaAcessoResponse`

Campos:

- `id`;
- `pessoaId`;
- `nomePessoa`;
- `login`;
- `status`;
- `perfis`.

O response não possui `senha` nem `senhaHash`.

## Serviço

`ContaAcessoService` concentra as regras de aplicação:

- valida `pessoaId`, login e senha;
- normaliza o login com `trim()`;
- rejeita login fora do padrão ou dos limites;
- rejeita senha fora dos limites de caracteres ou de bytes UTF-8;
- busca a pessoa e rejeita pessoa inexistente com `404`;
- rejeita pessoa inativa;
- rejeita pessoa que já possui conta;
- rejeita login já cadastrado, sem diferenciar caixa;
- gera o hash com `PasswordEncoder`;
- persiste somente o hash;
- na troca de senha, confere a senha atual com `matches`;
- rejeita nova senha igual à atual;
- persiste o novo hash.

O cadastro é transacional. A troca de senha também ocorre em transação.

## `UserDetailsService`

`ContaAcessoUserDetailsService` implementa `loadUserByUsername`:

- consulta o login sem diferenciar caixa;
- usa mensagem genérica `Login ou senha inválidos` quando o login não existe ou está em branco;
- converte cada perfil em autoridade com prefixo `ROLE_`, por exemplo `ROLE_OPERADOR`;
- usa o hash persistido como senha do `UserDetails`;
- desabilita a conta quando `podeAutenticar()` é falso, cobrindo conta bloqueada e pessoa inativa.

A mensagem genérica evita revelar se o login existe.

## Configuração de segurança

`SecurityConfig` define:

- `PasswordEncoder` delegado com BCrypt;
- `DaoAuthenticationProvider` com o `UserDetailsService` e o encoder;
- `SecurityFilterChain` com HTTP Basic;
- `SessionCreationPolicy.STATELESS`;
- CSRF desabilitado;
- regras explícitas de autorização.

### Rotas públicas

- `GET /api/health`;
- `/v3/api-docs/**`;
- `/swagger-ui.html`;
- `/swagger-ui/**`;
- `POST /api/pessoas`;
- `POST /api/contas-acesso`.

### Demais rotas

Todas as demais exigem autenticação.

O CSRF está desabilitado **somente** porque a API é stateless, não cria sessão e não usa cookie de autenticação. Se cookies forem introduzidos no futuro, o CSRF deve ser reavaliado.

## Endpoints

### Cadastrar conta

```http
POST /api/contas-acesso
Content-Type: application/json
```

Request:

```json
{
  "pessoaId": 1,
  "login": "ana.souza",
  "senha": "uma-frase-senha-2026"
}
```

Resposta `201 Created`:

```json
{
  "id": 1,
  "pessoaId": 1,
  "nomePessoa": "Ana Souza",
  "login": "ana.souza",
  "status": "ATIVA",
  "perfis": ["OPERADOR"]
}
```

A senha e o hash não aparecem na resposta. O cabeçalho `Location` aponta para o recurso.

### Consultar a própria conta

```http
GET /api/contas-acesso/me
Authorization: Basic <credencial>
```

Resposta `200 OK` com os mesmos campos públicos.

### Alterar a própria senha

```http
PUT /api/contas-acesso/me/senha
Authorization: Basic <credencial>
Content-Type: application/json
```

Request:

```json
{
  "senhaAtual": "uma-frase-senha-2026",
  "novaSenha": "outra-frase-senha-2026"
}
```

Resposta `204 No Content`.

A senha anterior deixa de autenticar. As rotas `/me` usam o login do principal autenticado; o cliente não informa o identificador da conta.

## Erros

### `401 Unauthorized`

Retornado quando:

- a rota é protegida e não há credencial;
- o login não existe;
- a senha está incorreta;
- a conta está bloqueada;
- a pessoa está inativa.

A resposta não revela qual condição falhou, não contém senha, hash ou stack trace e inclui `WWW-Authenticate: Basic`.

### `400 Bad Request`

Retornado quando:

- o login está fora dos limites ou do padrão;
- a senha está fora dos limites de caracteres ou de bytes;
- a pessoa está inativa;
- a senha atual está incorreta;
- a nova senha é igual à atual.

### `404 Not Found`

Retornado quando a pessoa informada no cadastro não existe.

### `409 Conflict`

Retornado quando:

- o login já está cadastrado;
- a pessoa já possui conta de acesso.

## OpenAPI

`OpenApiConfig` documenta:

- título e descrição da API;
- esquema `basicAuth` do tipo `http`, com `scheme: basic`;
- exigência global de segurança.

As operações públicas usam `@SecurityRequirements` vazio para remover a exigência global. Os endpoints de conta documentam:

- `201`, `400`, `404` e `409` no cadastro;
- `200` e `401` em `/me`;
- `204`, `400` e `401` na troca de senha.

Os schemas de request documentam a senha como `password` e `writeOnly`. O schema de response não possui `senha` nem `senhaHash`.

A descrição do esquema reforça que HTTP Basic deve ser usado somente sobre HTTPS/TLS.

## Postman

A collection existente foi atualizada:

```text
docs/postman/AL-Sports-Aula-07.postman_collection.json
```

Ela agora se chama `AL Sports - Aula 12` e usa HTTP Basic na raiz:

- `{{authLogin}}`;
- `{{authPassword}}`.

As rotas públicas usam `noauth`:

- `GET /api/health`;
- `GET /v3/api-docs`;
- `POST /api/pessoas`;
- `POST /api/contas-acesso`.

A pasta `06 - Aula 12` contém oito requests:

1. health público;
2. OpenAPI público;
3. criação de pessoa bootstrap;
4. criação de conta de acesso;
5. consulta da própria conta;
6. alteração de senha;
7. rota protegida sem credencial retornando 401;
8. senha incorreta retornando 401.

As variáveis `authLogin`, `authPassword` e `novaSenha` devem ser preenchidas localmente. A collection não contém credenciais reais e não deve ser exportada com valores preenchidos.

## Testes

### Unitários

- `ContaAcessoTest`: criação, normalização de login, limites, bloqueio, ativação, pessoa inativa, coleção imutável e ausência de hash em `toString()`;
- `ContaAcessoServiceTest`: cadastro com BCrypt, duplicidades, pessoa inexistente e inativa, perfil inicial, limites de senha e bytes, troca de senha, senha incorreta e nova senha igual;
- `ContaAcessoUserDetailsServiceTest`: login sem diferenciar caixa, autoridades `ROLE_*`, conta bloqueada, pessoa inativa e mensagem genérica.

### Integração e segurança

`ContaAcessoSecurityTest` mantém a cadeia de filtros ativa e usa HTTP Basic real. Cobre:

- health e OpenAPI públicos;
- 401 em rota protegida sem credencial;
- cadastro público de pessoa e conta;
- persistência de hash BCrypt;
- login sem diferenciar caixa;
- `/me` com credencial válida;
- senha incorreta, login inexistente, conta bloqueada e pessoa inativa;
- troca de senha com 204 e invalidação da senha anterior;
- erros 400, 404 e 409;
- ausência de senha, hash e stack trace nas respostas.

`OpenApiDocumentationTest` valida o esquema `basicAuth`, a exigência global, as operações públicas, os endpoints de conta e a ausência de `senha` e `senhaHash` no response.

Os testes anteriores que não avaliam segurança usam `@AutoConfigureMockMvc(addFilters = false)`. A cadeia de filtros não é desabilitada globalmente.

Para executar:

```powershell
.\mvnw.cmd clean test
```

A suíte de integração requer PostgreSQL e as variáveis de ambiente de teste.

## Limitações desta aula

Não fazem parte desta etapa:

- autorização diferenciada por perfil;
- JWT;
- OAuth2;
- login social;
- recuperação de senha;
- MFA;
- rate limiting;
- auditoria de tentativas de login;
- múltiplas contas por pessoa;
- escolha pública de perfil;
- bloqueio automático após tentativas inválidas.

Os perfis `OPERADOR` e `ADMINISTRADOR` existem no domínio e são convertidos em autoridades, mas as rotas ainda não diferenciam acesso por perfil. A concessão de `ADMINISTRADOR` é possível apenas por provisionamento administrativo futuro, não pelo cadastro público.

## HTTPS e TLS

HTTP Basic envia login e senha em cada requisição, codificados em Base64. Base64 não é criptografia: qualquer intermediário que veja a requisição pode ler as credenciais.

Por isso:

- a API deve ser publicada somente sobre HTTPS/TLS em produção;
- proxies reversos e balanceadores devem redirecionar HTTP para HTTPS;
- certificados devem ser válidos e renovados;
- logs de acesso não devem registrar o cabeçalho `Authorization`;
- credenciais não devem ser embutidas em URLs, collections compartilhadas ou código-fonte.

Em desenvolvimento local, HTTP é aceitável apenas para estudo.

## Checklist da aula

- [x] `ContaAcesso` separada de `Pessoa`, `Cliente`, `Colaborador` e `FuncaoColaborador`
- [x] Uma conta por pessoa nesta versão
- [x] Conta nova `ATIVA` e somente `OPERADOR`
- [x] Request público sem escolha de perfil
- [x] Hash BCrypt com prefixo `{bcrypt}`
- [x] Senha nunca persistida em texto aberto
- [x] Hash ausente de respostas, logs, erros e `toString()`
- [x] Migration `008` independente e incluída no changelog mestre
- [x] Login único normalizado por `LOWER(BTRIM(login))`
- [x] `UserDetailsService` integrado ao banco
- [x] HTTP Basic stateless
- [x] Rotas públicas explícitas
- [x] Demais rotas autenticadas
- [x] `401` genérico sem revelar existência de conta
- [x] Endpoints de cadastro, consulta e troca de senha
- [x] OpenAPI com `basicAuth`
- [x] Postman com Basic Auth na raiz e rotas públicas sem credencial
- [x] Testes unitários e de segurança
- [x] Documentação de limitações e HTTPS/TLS

## Próximos passos

A Aula 13 não faz parte desta entrega. A evolução natural é introduzir autorização diferenciada por perfil, seguida de mecanismos de token e recuperação de senha, sempre preservando a separação entre identidade, papel de negócio, função e credencial.

## Referências

- Spring Security — documentação oficial;
- BCrypt — algoritmo de hash de senhas;
- OWASP Password Storage Cheat Sheet;
- OWASP Authentication Cheat Sheet;
- RFC 7617 — The 'Basic' HTTP Authentication Scheme.
