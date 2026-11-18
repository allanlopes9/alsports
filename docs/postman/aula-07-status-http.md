# Status HTTP — API AL Sports

Documentação dos códigos HTTP e dos contratos exercitados pela collection acumulada `docs/postman/AL-Sports-Aula-17.postman_collection.json`.

Todos os endpoints da aplicação usam o prefixo `/api`. A documentação OpenAPI fica em `/v3/api-docs` e `/v3/api-docs.yaml`, e a interface Swagger UI em `/swagger-ui.html`.

> Nota documental: este arquivo mantém o nome histórico `aula-07-status-http.md`, mas seu conteúdo reflete a API e a collection no estado atual da Aula 17. Os registros históricos das aulas anteriores permanecem em seus próprios documentos.

## Convenções de resposta

As respostas de erro da aplicação usam `ApiError`, com os campos `timestamp`, `status`, `error`, `message`, `path` e `fields` (este último somente em erros de validação de campo). A API é stateless, não usa cookie de sessão e a autenticação das rotas protegidas é feita com Bearer Token JWT. Senhas, hashes, tokens e stack traces não são devolvidos ao cliente.

## HTTP 200 — OK

- `GET /api/health` — rota pública que devolve o corpo textual `OK` (`HealthController`);
- consultas protegidas, entre elas `GET /api/categorias`, `GET /api/fornecedores`, `GET /api/produtos`, `GET /api/pessoas`, `GET /api/clientes`, `GET /api/colaboradores`, `GET /api/vendas`, `GET /api/contas-acesso/me` e `GET /api/auditoria-seguranca`;
- alterações que devolvem a representação atualizada, como `PUT /api/produtos/{id}`, `PUT /api/pessoas/{id}` e `PUT /api/contas-acesso/{id}/status`;
- `POST /api/autenticacao/login` e `POST /api/autenticacao/refresh` — corpo `TokenResponse`, com `accessToken`, `tokenType`, `expiresIn`, `expiresAt`, `refreshToken` e `refreshTokenExpiresAt`.

## HTTP 201 — Created

Criação de recursos, com cabeçalho `Location` apontando para o recurso criado. Exemplos presentes na collection:

- `POST /api/categorias`
- `POST /api/fornecedores`
- `POST /api/produtos`
- `POST /api/pessoas`
- `POST /api/clientes`
- `POST /api/colaboradores`
- `POST /api/vendas`
- `POST /api/contas-acesso`

## HTTP 204 — No Content

Operações sem corpo de resposta verificadas na collection:

- `DELETE /api/categorias/{id}` e `DELETE /api/produtos/{id}`;
- `PUT /api/contas-acesso/me/senha`;
- `POST /api/autenticacao/logout` — logout local da família do refresh token;
- `POST /api/autenticacao/logout-todos` — logout global da conta, que exige Bearer access token.

## HTTP 400 — Bad Request

Corpo inválido, regra de negócio violada ou parâmetro de consulta inválido. Cenários exercitados na collection:

- produto com campos obrigatórios inválidos, código de barras vazio, quantidade negativa, preço inválido ou categoria inválida;
- JSON malformado em `POST /api/produtos`;
- ordenação inválida em `GET /api/produtos?sort=campoInexistente`;
- saída de estoque maior que o saldo disponível;
- CPF e CNPJ inválidos;
- quantidade com mais de três casas decimais;
- perfis vazios ou status ausente nas rotas administrativas de `/api/contas-acesso`;
- administrador tentando bloquear a própria conta ou remover o próprio perfil `ADMINISTRADOR`;
- paginação ou tipo inválido em `GET /api/auditoria-seguranca`: `page` deve ser maior ou igual a zero, `size` entre 1 e 100 e `tipo`, quando informado, deve pertencer ao enum `TipoEventoSeguranca`.

## HTTP 401 — Unauthorized

- rota protegida sem Bearer token ou com token inválido;
- `POST /api/autenticacao/login` — resposta genérica `Login ou senha inválidos` para login inexistente, senha incorreta, conta bloqueada ou pessoa inativa. Login em branco ou acima de 80 caracteres, e senha em branco ou acima de 72 caracteres, retornam 400;
- tentativa de login durante o bloqueio temporário da proteção de autenticação — também 401 genérico;
- `POST /api/autenticacao/refresh` — refresh token ausente, inválido, reutilizado, expirado ou de conta inativa;
- `GET /api/auditoria-seguranca` sem token.

A resposta 401 de login não revela qual condição ocorreu, nem o texto do login, da senha ou o hash de correlação.

## HTTP 403 — Forbidden

Autenticado sem o perfil exigido. Exemplos presentes na collection:

- `POST /api/contas-acesso` por operador;
- `DELETE /api/categorias/{id}` por operador;
- `GET /api/auditoria-seguranca` por operador.

A consulta de auditoria exige `ADMINISTRADOR` simultaneamente na política HTTP e em `@PreAuthorize`.

## HTTP 404 — Not Found

Recurso inexistente, devolvendo `ApiError` com `status: 404`. Exemplos da collection: `GET /api/produtos/999999999` e `DELETE /api/categorias/999999` por administrador.

## HTTP 409 — Conflict

Recurso duplicado ou vinculado a outros dados: código de barras já existente, categoria em uso e produto vendido que não pode ser excluído.

## HTTP 500 — Internal Server Error

Erro inesperado. O tratamento global evita expor stack traces, senhas, tokens, credenciais, comandos SQL ou detalhes da infraestrutura.

## Autenticação e ciclo de tokens

| Operação | Endpoint | Corpo da requisição | Sucesso |
|---|---|---|---|
| Login | `POST /api/autenticacao/login` | `{"login": "<login>", "senha": "<senha>"}` | 200 com o par de tokens |
| Renovação | `POST /api/autenticacao/refresh` | `{"refreshToken": "<refresh token atual>"}` | 200 com novo par da mesma família |
| Logout local | `POST /api/autenticacao/logout` | `{"refreshToken": "<refresh token atual>"}` | 204 |
| Logout global | `POST /api/autenticacao/logout-todos` | sem corpo; exige `Authorization: Bearer <access token>` | 204 |

`login`, `refresh` e `logout` são públicos na política HTTP; `logout-todos` exige Bearer access token válido. A renovação é de uso único: reapresentar um refresh token já utilizado revoga a família inteira e retorna 401. Access tokens já emitidos permanecem válidos até a expiração, conforme exercitado na collection. O endereço de origem registrado nos eventos é o observado na conexão pelo container, sem confiar em cabeçalhos encaminhados.

## Auditoria e proteção da autenticação

- `GET /api/auditoria-seguranca?tipo=LOGIN_FALHA&page=0&size=20` devolve 200 com uma página de `EventoSegurancaResponse`: `id`, `tipo`, `ocorridoEm`, `atorContaId`, `alvoContaId` e `enderecoOrigem`, em ordem estável, com os eventos mais recentes primeiro. O filtro por `tipo` é opcional.
- A resposta não contém `identificadorHash`, senhas, tokens, cabeçalhos nem corpos de requisição.
- O filtro é o enum fechado `TipoEventoSeguranca`: `LOGIN_SUCESSO`, `LOGIN_FALHA`, `LOGIN_RECUSADO_BLOQUEIO`, `BLOQUEIO_TEMPORARIO_ATIVADO`, `OPERACAO_RECUSADA`, `CONTA_CRIADA`, `SENHA_ALTERADA`, `PERFIS_ALTERADOS`, `STATUS_ALTERADO`, `REFRESH_SUCESSO`, `REFRESH_FALHA`, `LOGOUT` e `LOGOUT_GLOBAL`.
- Cinco falhas de login em 15 minutos bloqueiam novas tentativas por 15 minutos. O bloqueio temporário é correlacionado pelo hash SHA-256 do login normalizado e é distinto do status `BLOQUEADA` de `conta_acesso`.
- Falhas e recusas são registradas em transação independente para sobreviver ao HTTP 401. Sucesso de login e alterações administrativas confirmam ou revertem seus eventos junto com a operação.
- Não existe endpoint de escrita, alteração ou exclusão na trilha de auditoria: ela é produzida somente pelos casos de uso autenticados.

## Collection

Arquivo utilizado:

docs/postman/AL-Sports-Aula-17.postman_collection.json

A collection possui 89 requisições, 33 variáveis, autenticação Bearer na raiz, 86 eventos de script nas requisições e 1 evento de script na raiz. Inclui as pastas `10 - Autorização e ciclo de tokens` e `11 — Proteção e auditoria da autenticação`, além das pastas acumuladas das aulas anteriores. Nenhuma senha, token real ou credencial está registrada no arquivo: os valores são variáveis ou marcadores de exemplo.
