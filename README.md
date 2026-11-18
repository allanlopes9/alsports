# AL Sports

Sistema para controle de uma loja de artigos esportivos, desenvolvido como projeto acadêmico durante as Aulas 00 a 17 do curso de Java Spring Boot.

## Objetivo

O AL Sports tem como objetivo permitir o cadastro e o gerenciamento de produtos de uma loja de artigos esportivos, organizando os produtos por categoria e fornecedor.

O projeto foi desenvolvido de forma incremental, acompanhando os conceitos apresentados nas Aulas 00 a 17:

- configuração do ambiente;
- criação do projeto Spring Boot;
- modelagem do domínio;
- persistência com JPA;
- PostgreSQL;
- controle de schema com Liquibase;
- repositories;
- services;
- transações;
- evolução do modelo;
- DTOs;
- mapeadores;
- API REST;
- validação de dados;
- tratamento padronizado de erros;
- testes automatizados;
- testes da API com Postman;
- filtros, paginação e contrato OpenAPI;
- vendas, pessoas e estoque fracionado;
- concorrência, isolamento transacional e bloqueio pessimista;
- identidade de acesso, hash BCrypt e autenticação Bearer Token com JWT no Spring Security (OAuth2 Resource Server);
- autorização por perfis, `@PreAuthorize`, política HTTP e administração de contas;
- administrador inicial provisionado por configuração externa;
- ciclo de vida dos tokens com refresh token opaco e rotação segura;
- proteção persistente contra tentativas on-line de autenticação e trilha estruturada de eventos de segurança;
- estratégia de testes em camadas, JUnit Jupiter, Mockito e cobertura de código com JaCoCo.

## Tecnologias

- Java 21
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Security OAuth2 Resource Server
- JWT (HS256)
- Bean Validation
- PostgreSQL
- Liquibase
- Maven
- JUnit Jupiter
- Mockito
- MockMvc
- JaCoCo
- Postman

## Domínio

### Produto

O produto é a principal entidade do sistema e possui:

- `id`
- `codigoBarras`
- `nome`
- `quantidade`
- `estoqueMinimo`
- `preco`
- `dataCadastro`
- `ativo`
- `unidadeMedida`
- `categoria`
- `fornecedor`

O código de barras é obrigatório e único.

A quantidade em estoque é armazenada como `NUMERIC(18,3)`, aceita até três casas decimais e não pode ser negativa. O estoque mínimo também possui validação para valores não negativos.

### Categoria

A categoria organiza os produtos da loja.

Uma categoria pode possuir vários produtos, enquanto cada produto pertence a uma categoria.

### Fornecedor

O fornecedor representa a empresa responsável pelo fornecimento dos produtos.

Um produto pode possuir um fornecedor associado.

### Pessoa, Cliente e Colaborador

`Pessoa` é a identidade civil central do sistema, com nome, e-mail, CPF e status. Os papéis de negócio são separados:

- `Cliente` representa a pessoa que compra;
- `Colaborador` representa a pessoa que trabalha na loja;
- `FuncaoColaborador` representa a função profissional exercida.

Uma mesma pessoa pode ser cliente ou colaboradora sem possuir acesso ao sistema. O papel de negócio não concede, por si só, autorização para usar a API.

### Conta de acesso

`ContaAcesso` é a credencial de autenticação associada a uma pessoa:

- `id`;
- `pessoa` — relação um-para-um, com no máximo uma conta por pessoa nesta versão;
- `login` — único sem diferenciar maiúsculas, minúsculas ou espaços externos;
- `senhaHash` — somente o hash BCrypt, nunca a senha em texto aberto;
- `status` — `ATIVA` ou `BLOQUEADA`;
- `perfis` — `OPERADOR` ou `ADMINISTRADOR`.

O perfil de acesso é separado da função profissional: `FuncaoColaborador` descreve o trabalho exercido, enquanto `PerfilAcesso` descreve o que a conta pode fazer na API.

A partir da Aula 14, o cadastro de conta é uma operação administrativa: o administrador cria a conta com status `ATIVA` e perfil inicial `OPERADOR`. Os perfis podem ser substituídos depois por outro administrador. A conta nunca fica sem ao menos um perfil. Uma conta bloqueada ou vinculada a uma pessoa inativa não autentica.

A partir da Aula 17, existe também um **bloqueio temporário automático**, distinto do bloqueio administrativo acima. Cinco falhas em 15 minutos bloqueiam o identificador por 15 minutos. O estado fica em `protecao_autenticacao`, correlacionado apenas pelo hash SHA-256 do login normalizado; nenhum login em texto aberto é gravado. O acesso é liberado automaticamente ao fim do prazo.

## Organização do projeto

O projeto utiliza separação de responsabilidades entre as principais camadas:

src/
├── main/
│   ├── java/com/curso/alsports/
│   │   ├── api/
│   │   ├── controller/
│   │   ├── domain/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   └── service/
│   │
│   └── resources/
│       ├── db/changelog/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       └── application-schema-reference.properties
│
└── test/
    ├── java/com/curso/alsports/
    └── resources/
        └── application-test.properties

### api

Contém endpoints básicos da aplicação, como o endpoint de saúde.

### controller

Responsável pelos endpoints REST da aplicação.

Atualmente existem controllers para:

- categorias;
- fornecedores;
- produtos;
- pessoas, clientes e colaboradores;
- vendas;
- contas de acesso;
- auditoria de segurança;
- health check.

### domain

Contém o domínio independente da persistência JPA.

O pacote possui:

- `Produto`;
- `CategoriaProduto`;
- `Status`.

Essa separação permite manter as regras e conceitos do domínio independentes dos detalhes de persistência.

### model

Contém as entidades JPA utilizadas para persistência no PostgreSQL.

### dto

Contém os objetos utilizados na comunicação da API.

São utilizados:

- Request DTOs;
- Response DTOs;
- Mappers.

Os controllers não expõem diretamente as entidades JPA como contrato da API.

### exception

Centraliza o tratamento de erros da API.

Principais componentes:

- `ErroResponse`;
- `RecursoDuplicadoException`;
- `RecursoNaoEncontradoException`;
- `GlobalExceptionHandler`.

### repository

Contém os repositories Spring Data JPA responsáveis pelo acesso aos dados.

Repositories atuais:

- `ProdutoRepository`;
- `CategoriaProdutoRepository`;
- `FornecedorRepository`;
- `PessoaRepository`;
- `ClienteRepository`;
- `ColaboradorRepository`;
- `VendaRepository`;
- `ContaAcessoRepository`;
- `SessaoRefreshTokenRepository`;
- `ProtecaoAutenticacaoRepository`;
- `EventoSegurancaRepository`.

O `ContaAcessoRepository` oferece consultas de unicidade por login sem diferenciar caixa e por pessoa, além de carregar a pessoa e os perfis com `@EntityGraph` na autenticação. O `ProtecaoAutenticacaoRepository` faz o upsert por hash e a consulta com `PESSIMISTIC_WRITE`; o `EventoSegurancaRepository` pagina a trilha com filtro opcional por tipo.

### service

Contém as regras de aplicação e operações de negócio.

Services atuais:

- `ProdutoService`;
- `CategoriaProdutoService`;
- `FornecedorService`;
- `PessoaService`;
- `ClienteService`;
- `ColaboradorService`;
- `VendaService`;
- `ContaAcessoService`;
- `ContaAcessoUserDetailsService`;
- `SessaoAutenticacaoService`;
- `ProtecaoAutenticacaoService`;
- `AuditoriaSegurancaService`.

As operações de persistência que precisam de transação utilizam `@Transactional`. As interfaces `ProtecaoLogin` e `RegistradorEventosSeguranca` separam os casos de uso da infraestrutura de proteção e auditoria. Falhas e recusas de login usam `REQUIRES_NEW` para sobreviver ao HTTP 401; sucesso e alterações administrativas participam da transação do chamador, de modo que alteração e evento confirmam ou revertem juntos.

## Banco de dados

O projeto utiliza PostgreSQL como banco de dados.

A evolução do schema é controlada pelo Liquibase.

O Hibernate está configurado com:

spring.jpa.hibernate.ddl-auto=validate

Isso significa que o Hibernate valida o schema existente, enquanto as alterações estruturais do banco são realizadas pelas migrations do Liquibase.

## Liquibase

As migrations estão localizadas em:

src/main/resources/db/changelog/changes/

Atualmente existem:

001-create-categoria-produto.yaml
002-create-produto.yaml
003-add-fk-produto-categoria.yaml
003-fornecedor-e-estoque-minimo.yaml
004-add-constraints-produto.yaml
005-add-codigo-barras-produto.yaml
005-pessoas-clientes-vendas.yaml
006-consolidate-categorias.yaml
007-quantidade-decimal.yaml
008-contas-acesso.yaml
009-sessoes-refresh-token.yaml
010-auditoria-protecao-autenticacao.yaml

Entre as alterações realizadas estão:

- criação da tabela de categorias;
- criação da tabela de produtos;
- relacionamento entre produto e categoria;
- criação de fornecedor;
- criação do estoque mínimo;
- restrição de nome de produto;
- validação de quantidade não negativa;
- criação do código de barras;
- preenchimento dos códigos de barras existentes;
- código de barras obrigatório;
- código de barras único;
- consolidação de categorias duplicadas;
- nome de categoria único, ignorando maiúsculas, minúsculas e espaços nas extremidades;
- criação das tabelas de pessoas, clientes, colaboradores, vendas e itens de venda;
- conversão de `produto.quantidade` e `item_venda.quantidade` para `NUMERIC(18,3)`;
- criação das tabelas `conta_acesso` e `conta_acesso_perfil`, com uma conta por pessoa, login único normalizado, status conhecido e perfis restritos.

O changeset `007-quantidade-decimal.yaml` converte as colunas `INTEGER` para `NUMERIC(18,3)` com `USING quantidade::NUMERIC(18,3)`, sem perda dos valores inteiros existentes. As constraints `ck_produto_quantidade` (`quantidade >= 0`) e `ck_item_venda_quantidade` (`quantidade > 0`) são recriadas após a conversão.

O rollback desses changesets converte as colunas de volta para `INTEGER` e recria as constraints. Como a conversão para inteiro arredondaria frações, o rollback é bloqueado por uma verificação que falha caso existam quantidades fracionadas gravadas em `produto.quantidade` ou `item_venda.quantidade`.

A migration `008-contas-acesso.yaml` cria `conta_acesso` (`id`, `pessoa_id`, `login`, `senha_hash`, `status`) e `conta_acesso_perfil` (`conta_acesso_id`, `perfil`, com chave primária composta). Ela adiciona:

- chave estrangeira de `conta_acesso.pessoa_id` para `pessoa.id`, com `ON DELETE RESTRICT`;
- restrição de unicidade de `pessoa_id`, garantindo uma conta por pessoa nesta versão;
- índice único `uk_conta_acesso_login_lower` sobre `LOWER(BTRIM(login))`;
- `CHECK` de status (`ATIVA`, `BLOQUEADA`), de tamanho do login e de perfil (`OPERADOR`, `ADMINISTRADOR`);
- chave estrangeira de `conta_acesso_perfil.conta_acesso_id` para `conta_acesso.id`, com `ON DELETE CASCADE`.

A migration não cria contas, senhas ou hashes reais. O rollback remove as constraints e as tabelas na ordem inversa.

A migration `009-sessoes-refresh-token.yaml` acrescenta três changesets para a família de sessões: tabela `sessao_refresh_token`, chave estrangeira e índices, e restrições de unicidade do hash e dos motivos de revogação. Somente o hash SHA-256 do refresh token é persistido.

A migration `010-auditoria-protecao-autenticacao.yaml` acrescenta três changesets:

- `protecao_autenticacao`, com `chave_login` única no formato `[0-9a-f]{64}`, contador não negativo, janela e prazo de bloqueio;
- `evento_seguranca`, com tipo enum fechado por `CHECK`, IDs históricos de conta, hash de correlação opcional e origem de até 45 caracteres sem CR/LF;
- índices para bloqueios ativos, ordenação por ocorrência e consulta filtrada por tipo.

Todos os changesets da 010 têm rollback explícito. O histórico real passa de 43 para 46 changesets. A aplicação, o rollback e a reaplicação são verificados em um banco PostgreSQL temporário exclusivo, sem apagar o banco de testes compartilhado.

O arquivo principal das migrations é:

src/main/resources/db/changelog/db.changelog-master.yaml

## Configuração

As informações de acesso ao banco são fornecidas por variáveis de ambiente.

O projeto possui:

.env
.env.example

O arquivo `.env` é utilizado somente localmente e não deve ser versionado.

O `.env.example` contém apenas placeholders e serve como referência para a configuração do ambiente.

### Segurança

Nunca devem ser versionados:

- senhas;
- tokens;
- chaves privadas;
- chave JWT (`JWT_SECRET`);
- credenciais;
- arquivos `.env`;
- outros segredos reais.

O `.gitignore` possui a regra:

.env

### Autenticação e proteção de senhas

A partir da Aula 13, a API usa Spring Security como OAuth2 Resource Server e autentica as rotas protegidas com Bearer Token JWT. A partir da Aula 14, a autorização é decidida pelos perfis `OPERADOR` e `ADMINISTRADOR`:

- `POST /api/autenticacao/login` é público e emite o token;
- `GET /api/health` é público;
- `GET /v3/api-docs/**`, `/v3/api-docs.yaml`, `/swagger-ui.html` e `/swagger-ui/**` são públicos;
- `POST /api/pessoas` e `POST /api/contas-acesso` não são mais públicos;
- `POST /api/contas-acesso`, `PUT /api/contas-acesso/*/perfis`, `PUT /api/contas-acesso/*/status` e `DELETE /api/**` exigem `ADMINISTRADOR`;
- as demais rotas de `/api/**` exigem `OPERADOR` ou `ADMINISTRADOR`;
- qualquer rota não classificada termina em `denyAll()`;
- a autenticação é stateless, sem sessão e sem cookie;
- o CSRF está desabilitado somente porque a API não mantém sessão nem cookie de autenticação;
- HTTP Basic foi removido e não autentica mais nenhuma rota.

A chave HMAC é lida de `JWT_SECRET`, nunca do código-fonte ou de arquivos versionados. O valor deve estar em Base64 e possuir pelo menos 32 bytes após a decodificação. A aplicação falha na inicialização se a chave estiver ausente, não for Base64 ou for curta demais. O `.env.example` contém apenas um placeholder fictício.

A configuração tipada fica em `app.security.jwt`:

- `issuer` — emissor didático `https://alsports.local`;
- `expiration` — `PT30M`;
- `secret` — `${JWT_SECRET}`.

O algoritmo é fixado em HS256 no encoder e no decoder. O Resource Server valida assinatura, emissor e expiração; o cabeçalho do token não escolhe livremente o algoritmo.

O access token tem validade absoluta de 30 minutos: `exp = iat + PT30M`. O uso não renova o prazo. Desde a Aula 16 existe refresh token opaco de 256 bits, persistido apenas como hash SHA-256, com rotação de uso único, detecção de reutilização, inatividade de 30 minutos e limite absoluto de 8 horas. Há logout local e global da família de sessões. Não há denylist nem revogação de JWT já emitido por `jti`: um access token emitido continua válido até `exp`, mesmo após bloqueio da conta, troca de senha ou logout.

O login é comparado sem diferenciar maiúsculas, minúsculas ou espaços externos. Tentativas com login inexistente, senha incorreta, conta bloqueada administrativamente, pessoa inativa ou bloqueio temporário ativo recebem a mesma resposta genérica `401 Unauthorized` com a mensagem `Login ou senha inválidos`, sem revelar qual condição falhou.

As senhas continuam persistidas com `PasswordEncoderFactories.createDelegatingPasswordEncoder()`, que produz hashes BCrypt prefixados com `{bcrypt}`. O hash inclui salt aleatório e fator de trabalho. A senha em texto aberto nunca é armazenada, devolvida, registrada em log ou incluída no token.

Um JWT assinado não é criptografado. O payload é apenas codificado em Base64URL e pode ser lido por qualquer pessoa que possua o token; a assinatura garante integridade e autenticidade, não sigilo. Por isso o token contém somente `iss`, `sub`, `iat`, `exp`, `jti` e `perfis`, sem senha, hash, CPF ou outros dados desnecessários.

Esta aplicação é um Resource Server e também um emissor didático de tokens para o próprio login. Ela não é um Authorization Server completo: não implementa OAuth2, não expõe JWK Set, não usa RSA/RS256 e não atende outros clientes.

### Proteção on-line e auditoria (Aula 17)

A política padrão é `5` falhas em `PT15M`, com bloqueio automático de `PT15M`. Ela é configurada em `app.security.authentication-protection` por propriedades tipadas e validadas: valores menores que um, janela ou duração nulas/zero/negativas impedem a inicialização. As variáveis `AUTHENTICATION_MAX_FAILED_ATTEMPTS`, `AUTHENTICATION_FAILURE_WINDOW` e `AUTHENTICATION_LOCK_DURATION` não são segredos.

O identificador é normalizado e correlacionado apenas por SHA-256 hexadecimal de 64 caracteres. Falhas e recusas são gravadas em transação própria (`REQUIRES_NEW`) para sobreviver ao HTTP 401; sucesso e alterações administrativas usam a transação do chamador. Contador, janela e bloqueio ficam no PostgreSQL, compartilhados entre instâncias.

Os eventos `LOGIN_SUCESSO`, `LOGIN_FALHA`, `LOGIN_RECUSADO_BLOQUEIO`, `BLOQUEIO_TEMPORARIO_ATIVADO`, `CONTA_CRIADA`, `SENHA_ALTERADA`, `PERFIS_ALTERADOS`, `STATUS_ALTERADO`, `REFRESH_SUCESSO`, `REFRESH_FALHA`, `LOGOUT` e `LOGOUT_GLOBAL` são estruturados, sem mensagem livre, senha, hash BCrypt, tokens, cabeçalhos ou corpos de requisição. A origem é limitada a 45 caracteres e rejeita CR/LF.

Não há WAF, CAPTCHA, MFA, SIEM, proteção distribuída de borda ou não repúdio criptográfico. A trilha não é assinada e o bloqueio pode ser usado como negação de serviço; limites e retenção devem ser avaliados por contexto.

### Autorização por perfis

A partir da Aula 14, o claim `perfis` do JWT é convertido em authorities `ROLE_OPERADOR` e `ROLE_ADMINISTRADOR` por um `JwtAuthenticationConverter` com `JwtGrantedAuthoritiesConverter`. O claim continua sem o prefixo `ROLE_`; o prefixo é convenção do Spring Security.

A política é aplicada em duas camadas:

- na cadeia HTTP, antes do controller;
- no service, com `@EnableMethodSecurity` e `@PreAuthorize("hasRole('ADMINISTRADOR')")` em `cadastrar`, `alterarPerfis`, `alterarStatus` e na consulta de auditoria.

`401` indica ausência de autenticação válida; `403` indica identidade autenticada sem o perfil exigido.

Invariantes administrativas:

- a conta sempre mantém ao menos um perfil;
- o administrador não remove o próprio `ADMINISTRADOR`;
- o administrador não bloqueia a própria conta;
- o último administrador ativo não pode ser bloqueado nem perder o perfil;
- violações retornam `400 Bad Request` com mensagem estável.

O administrador inicial é opcional e desativado por padrão. Quando `INITIAL_ADMIN_ENABLED=true`, as propriedades `INITIAL_ADMIN_*` criam uma única `Pessoa` e `ContaAcesso` com `OPERADOR` e `ADMINISTRADOR`, gravando apenas o hash BCrypt. O processo é idempotente e deve ser desligado após a primeira execução. Nenhuma senha real é versionada.

Um token emitido antes de uma alteração de perfil ou bloqueio continua válido até `exp` e preserva os perfis do momento da emissão. Não há revogação nesta aula.

Em produção, a API deve ser publicada exclusivamente sobre HTTPS/TLS. Sem TLS, o Bearer Token pode ser interceptado e reutilizado até expirar.

## API REST

### Health Check

GET /api/health

Resposta:

OK

### Categorias

Criar categoria:

POST /api/categorias

Listar categorias:

GET /api/categorias

Buscar categoria:

GET /api/categorias/{id}

### Fornecedores

Criar fornecedor:

POST /api/fornecedores

Listar fornecedores:

GET /api/fornecedores

Buscar fornecedor:

GET /api/fornecedores/{id}

### Produtos

Criar produto:

POST /api/produtos

Listar produtos:

GET /api/produtos

Buscar produto:

GET /api/produtos/{id}

O projeto também possui operações adicionais de atualização e exclusão de produtos.

### Contas de acesso

Cadastrar conta (ADMINISTRADOR):

POST /api/contas-acesso

Exemplo de request:

{
    "pessoaId": 1,
    "login": "ana.souza",
    "senha": "uma-frase-senha-2026"
}

A resposta `201 Created` contém `id`, `pessoaId`, `nomePessoa`, `login`, `status` e `perfis`. A senha e o hash nunca são devolvidos. A conta é criada `ATIVA` e somente com `OPERADOR`.

Substituir perfis (ADMINISTRADOR):

PUT /api/contas-acesso/{id}/perfis

Exemplo de request:

{
    "perfis": ["OPERADOR", "ADMINISTRADOR"]
}

A substituição é integral. A lista não pode ser vazia. O administrador autenticado não pode remover de si mesmo `ADMINISTRADOR`, e o último administrador ativo não pode perder o perfil. Violações retornam `400 Bad Request`.

Ativar ou bloquear conta (ADMINISTRADOR):

PUT /api/contas-acesso/{id}/status

Exemplo de request:

{
    "status": "BLOQUEADA"
}

`BLOQUEADA` impede novos logins. O administrador não pode bloquear a própria conta e o último administrador ativo não pode ser bloqueado. Violações retornam `400 Bad Request`. A reativação usa `ATIVA`.

Consultar a própria conta (autenticado):

GET /api/contas-acesso/me

Alterar a própria senha (autenticado):

PUT /api/contas-acesso/me/senha

Exemplo de request:

{
    "senhaAtual": "uma-frase-senha-2026",
    "novaSenha": "outra-frase-senha-2026"
}

A resposta é `204 No Content`. A senha atual é conferida, a nova senha precisa ser diferente e o servidor persiste somente o novo hash.

As rotas `/me` usam o login do usuário autenticado no Bearer Token (claim `sub`); o cliente não informa o identificador da conta.

### Login e autenticação das demais rotas

O login é público e troca credenciais válidas por um access token JWT:

POST /api/autenticacao/login

Exemplo de request:

{
    "login": "ana.souza",
    "senha": "uma-frase-senha-2026"
}

Exemplo de resposta `200 OK`:

{
    "accessToken": "<jwt-assinado>",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "expiresAt": "2026-11-17T21:30:00Z"
}

Credenciais inválidas, conta bloqueada administrativamente, pessoa inativa ou bloqueio temporário ativo retornam `401 Unauthorized` com o mesmo `ApiError` genérico e a mensagem `Login ou senha inválidos`. Login ou senha em branco e valores acima dos limites retornam `400 Bad Request`. Após cinco falhas em 15 minutos, a sexta tentativa com a senha correta também recebe `401` até o fim do bloqueio.

As rotas de categorias, fornecedores, produtos, pessoas, clientes, colaboradores, vendas, estoque e contas de acesso exigem Bearer Token:

Authorization: Bearer <token>

As rotas cotidianas aceitam `OPERADOR` ou `ADMINISTRADOR`. Criação de conta, alteração de perfis ou status e exclusões exigem `ADMINISTRADOR`; um token válido sem esse perfil recebe `403 Forbidden`.

Sem token, com token malformado, adulterado, expirado, assinado com outra chave ou com emissor incorreto, a resposta é `401 Unauthorized` com o cabeçalho `WWW-Authenticate: Bearer`. O Resource Server não informa se o token existiu, se foi revogado ou qual validação falhou. HTTP Basic também retorna `401`.

A resposta de login também contém `refreshToken` e `refreshTokenExpiresAt`. A renovação usa:

POST /api/autenticacao/refresh

{ "refreshToken": "<valor opaco>" }

O encerramento local usa `POST /api/autenticacao/logout` com o mesmo corpo; o global usa `POST /api/autenticacao/logout-todos` com Bearer Token. Um access token já emitido permanece válido até `exp`.

### Auditoria de segurança

Consultar eventos (ADMINISTRADOR):

GET /api/auditoria-seguranca?page=0&size=20&tipo=LOGIN_FALHA

- `page` começa em zero e `size` vai de 1 a 100;
- `tipo` é opcional e aceita apenas os valores do enum de eventos;
- a ordenação é estável: `ocorrido_em DESC, id DESC`;
- a resposta é uma página Spring (`content`, `totalElements`, `totalPages`) com `id`, `tipo`, `ocorridoEm`, `atorContaId`, `alvoContaId` e `enderecoOrigem`;
- não há `identificadorHash`, senha, token ou mensagem livre na resposta;
- OPERADOR recebe `403`, ausência de token recebe `401` e parâmetros inválidos recebem `400`;
- não existem endpoints de escrita, alteração ou exclusão da trilha.

## DTO de produto

A criação de produtos utiliza um DTO específico para entrada.

Exemplo:

{
    "codigoBarras": "POSTMAN-BOLA-001",
    "nome": "Bola Postman",
    "quantidade": 10.000,
    "estoqueMinimo": 2.000,
    "preco": 99.90,
    "dataCadastro": "2026-09-07",
    "ativo": true,
    "unidadeMedida": "UN",
    "categoriaId": 1,
    "fornecedorId": 1
}

O DTO possui validações para garantir que os dados recebidos sejam válidos antes da execução da regra de negócio.

## Quantidades fracionadas

A partir da Aula 10, as quantidades de produto e de item de venda são decimais, com até três casas decimais:

- `produto.quantidade`: `NUMERIC(18,3)`, aceita `0.000` e valores positivos;
- `item_venda.quantidade`: `NUMERIC(18,3)`, aceita valores positivos a partir de `0.001`.

O valor máximo aceito é `999999999999999.999` (15 dígitos inteiros e 3 decimais). Valores com mais de três casas decimais são rejeitados, sem arredondamento silencioso.

Exemplo de item de venda fracionado:

{
    "produtoId": 1,
    "quantidade": 1.500
}

O subtotal do item é calculado multiplicando a quantidade fracionada pelo preço unitário registrado no momento da venda. O total da venda soma os subtotais e é arredondado para duas casas decimais com `HALF_UP`.

## Concorrência e bloqueio de estoque

A partir da Aula 11, todas as operações que alteram saldo coordenam o acesso ao produto no banco de dados:

- finalização de venda;
- recebimento manual de estoque;
- retirada manual de estoque.

A finalização de uma venda executa dentro de uma única transação (`@Transactional`). Antes de validar o status do produto e o saldo disponível, o serviço atualiza a entidade com `EntityManager.refresh(produto, LockModeType.PESSIMISTIC_WRITE)`. O bloqueio pessimista de escrita é mantido até o fim da transação, de modo que outra operação concorrente sobre o mesmo produto aguarda a conclusão da primeira.

Os itens da venda são processados em ordem crescente de `produto.id`, independentemente da ordem em que foram incluídos. Essa ordenação determinística reduz o risco de deadlock quando duas vendas compartilham mais de um produto. A baixa de estoque de cada item ocorre somente depois da aquisição do bloqueio, e a venda só passa para `FINALIZADA` depois que todos os itens foram processados. Se qualquer item não possuir saldo suficiente ou estiver inativo, a transação é desfeita por completo.

As operações de entrada e saída manual também bloqueiam o produto antes de somar ou subtrair a quantidade. Consultas, listagens e alterações de preço ou descrição não adquirem bloqueio.

O comportamento é validado por testes que executam chamadas reais e simultâneas aos beans transacionais em PostgreSQL, com `ExecutorService`, `CountDownLatch` e timeout nos `Future`.

## Validações

Entre as validações implementadas estão:

- código de barras obrigatório;
- código de barras com no máximo 50 caracteres;
- nome obrigatório;
- nome com no máximo 150 caracteres;
- quantidade obrigatória;
- quantidade não negativa;
- quantidade com no máximo 3 casas decimais;
- quantidade dentro da precisão `NUMERIC(18,3)`;
- quantidade de item de venda positiva, com mínimo `0.001`;
- estoque mínimo obrigatório;
- estoque mínimo não negativo;
- preço obrigatório;
- preço não negativo;
- data de cadastro obrigatória;
- status ativo/inativo obrigatório;
- unidade de medida obrigatória;
- unidade de medida com no máximo 30 caracteres;
- categoria obrigatória;
- categoria com identificador positivo;
- fornecedor opcional, quando informado deve possuir identificador positivo.

## Tratamento de erros

A API possui tratamento global de exceções.

### HTTP 400 — Bad Request

Utilizado quando os dados enviados são inválidos ou o JSON está malformado.

Exemplos:

- campos obrigatórios ausentes;
- valores inválidos;
- quantidade negativa;
- quantidade com mais de três casas decimais;
- quantidade acima da precisão permitida;
- JSON inválido.

### HTTP 404 — Not Found

Utilizado quando o recurso solicitado não existe.

Exemplo:

GET /api/produtos/999999999

### HTTP 409 — Conflict

Utilizado quando existe conflito com um recurso já cadastrado.

Exemplos:

- código de barras duplicado;
- nome de produto duplicado;
- CNPJ de fornecedor já cadastrado;
- login de conta de acesso duplicado;
- pessoa que já possui conta de acesso.

### HTTP 401 — Unauthorized

Utilizado quando a credencial está ausente ou inválida.

Exemplos em rotas protegidas:

- requisição sem cabeçalho `Authorization`;
- token malformado, adulterado ou expirado;
- token assinado com outra chave ou com emissor incorreto;
- tentativa de autenticação com HTTP Basic;
- token ausente após a expiração absoluta de 30 minutos.

Exemplos no endpoint de login:

- login inexistente;
- senha incorreta;
- conta bloqueada administrativamente;
- pessoa vinculada à conta está inativa;
- bloqueio temporário ativo após cinco falhas;
- senha correta apresentada durante o bloqueio temporário.

No login, a resposta é um `ApiError` genérico com a mensagem `Login ou senha inválidos`; ela não revela senha, hash, existência do login, duração do bloqueio ou motivo específico da falha. Nas rotas protegidas, o Resource Server mantém o comportamento padrão do Spring Security, com `WWW-Authenticate: Bearer` e sem detalhes internos.

Um access token já emitido continua válido até `exp`, mesmo que a conta seja bloqueada, a senha seja alterada ou a família de refresh seja encerrada. O bloqueio temporário da Aula 17 impede novos logins, não revoga tokens emitidos.

### HTTP 500 — Internal Server Error

Utilizado para erros inesperados da aplicação.

Informações sensíveis, SQL, senhas e stack traces não são expostos pela API.

## Testes automatizados

O projeto possui testes para diferentes camadas.

### Testes do domínio

Localizados em:

src/test/java/com/curso/alsports/domain/

Incluem:

- `ProdutoTest`;
- `CategoriaProdutoTest`.

### Testes da aplicação e API

Localizados em:

src/test/java/com/curso/alsports/AlsportsApplicationTests.java

Os testes utilizam Spring Boot e MockMvc para validar o comportamento da API.

São testados cenários como:

- criação de produto;
- busca de produto;
- listagem;
- validação;
- recurso inexistente;
- código de barras duplicado;
- JSON inválido;
- persistência;
- transações;
- categorias;
- fornecedores.

### Testes de serviços

Localizados em:

src/test/java/com/curso/alsports/service/

Os testes de `ProdutoServiceTest` e `VendaServiceTest` usam Mockito para verificar a ordem dos bloqueios, a aquisição do lock antes da baixa, a preservação do saldo em caso de falha e a manutenção da venda em `ABERTA` quando algum item não pode ser processado.

### Testes de concorrência

Localizados em:

src/test/java/com/curso/alsports/Aula11ConcorrenciaTests.java

A classe não abre transação de teste. Cada chamada aos serviços parte de uma thread própria e atravessa o proxy do Spring, gerando transações independentes em PostgreSQL. Os cenários cobrem:

- duas finalizações disputando o mesmo saldo;
- finalização concorrente com retirada manual;
- finalização concorrente com entrada manual;
- rollback de venda com múltiplos itens quando um deles não possui saldo;
- vendas com produtos em ordens opostas, sem deadlock;
- saldo fracionado em finalizações concorrentes.

Os dados criados por cada teste são removidos ao final.

### Testes de segurança e JWT

Localizados em:

- `src/test/java/com/curso/alsports/AutenticacaoSecurityTest.java`;
- `src/test/java/com/curso/alsports/AutorizacaoSecurityTest.java`;
- `src/test/java/com/curso/alsports/ContaAcessoSecurityTest.java`;
- `src/test/java/com/curso/alsports/config/JwtConfigTest.java`;
- `src/test/java/com/curso/alsports/config/JwtAuthenticationConverterTest.java`;
- `src/test/java/com/curso/alsports/service/TokenServiceTest.java`;
- `src/test/java/com/curso/alsports/service/AutenticacaoServiceTest.java`;
- `src/test/java/com/curso/alsports/service/AdministradorInicialServiceTest.java`.

As classes de integração mantêm a cadeia de filtros do Spring Security ativa e usam MockMvc com Bearer Token real, emitido pelo endpoint de login. Os cenários cobrem:

- health, OpenAPI e login públicos;
- cadastro de pessoa e conta exigindo `ADMINISTRADOR`;
- claim `perfis` convertido em `ROLE_OPERADOR` e `ROLE_ADMINISTRADOR`, sem duplicar o prefixo;
- token sem o claim `perfis` sem authorities de negócio;
- operador acessando rotas cotidianas e recebendo 403 nas administrativas;
- administrador criando conta, alterando perfis e alterando status;
- lista vazia de perfis e status ausente retornando 400;
- autobloqueio e autorremoção retornando 400;
- último administrador ativo protegido contra bloqueio e remoção de perfil;
- provisionamento inicial criando a conta uma única vez e concedendo os dois perfis;
- provisionamento rejeitando CPF inválido, campos vazios e duplicidades;
- emissão de token com contrato `accessToken`, `tokenType`, `expiresIn` e `expiresAt`;
- claims `iss`, `sub`, `iat`, `exp`, `jti` e `perfis`;
- validade absoluta de 1800 segundos sem renovação pelo uso;
- ausência de senha, hash, CPF e segredo no payload;
- rotas protegidas sem token retornando 401 com `WWW-Authenticate: Bearer`;
- HTTP Basic retornando 401;
- token malformado, adulterado, expirado, com emissor incorreto, assinado com outra chave ou com `alg: none` retornando 401;
- `/me` e rotas de produtos com token válido;
- senha incorreta, login inexistente, conta bloqueada e pessoa inativa retornando 401 genérico;
- validação estrutural do login retornando 400;
- token ainda válido após bloqueio da conta e após troca de senha, documentando a ausência de revogação;
- troca de senha com 204 e invalidação da senha anterior para novos logins;
- ausência de senha, hash e stack trace nas respostas.

`JwtConfigTest` valida a chave Base64, o mínimo de 32 bytes, a recusa de configuração inválida e a rejeição de tokens com emissor errado, expirados, adulterados, malformados, com `alg: none` ou assinados com outra chave. `TokenServiceTest` valida a emissão HS256, as claims, o `jti` único e a expiração absoluta. `AutenticacaoServiceTest` garante que o `AuthenticationManager` da Aula 12 é reutilizado e que nenhum token é emitido quando a autenticação falha. `JwtAuthenticationConverterTest` verifica o claim `perfis` e ignora a authority `FACTOR_BEARER` acrescentada pelo Resource Server. `AutorizacaoSecurityTest` cobre a política HTTP e a defesa em profundidade. `AdministradorInicialServiceTest` cobre criação, idempotência, validações e duplicidades do bootstrap.

Os testes unitários de `ContaAcessoTest`, `ContaAcessoServiceTest` e `ContaAcessoUserDetailsServiceTest` continuam validando as regras de domínio, a geração de hash, a comparação de senhas, a substituição de perfis e o mapeamento de perfis para autoridades.

A suíte possuía 398 testes aprovados no marco da Aula 16. A Aula 16 havia acrescentado `SessaoRefreshTokenTest` (16), `RefreshTokenPropertiesTest` (3), `CicloTokenApiTest` (23) e `CicloTokenConcorrenciaTest` (1), sem remover testes existentes. A Aula 17 acrescenta testes de domínio, integração HTTP, concorrência, migration/rollback e auditoria obrigatória, também sem remover ou desabilitar testes anteriores. Os testes que não avaliam segurança usam `@AutoConfigureMockMvc(addFilters = false)` no próprio caso de uso. A cadeia de filtros não é desabilitada globalmente; `AutenticacaoSecurityTest`, `AutorizacaoSecurityTest`, `ContaAcessoSecurityTest`, `CicloTokenApiTest`, `CicloTokenConcorrenciaTest` e `OpenApiDocumentationTest` permanecem com os filtros ativos.

### Testes de proteção e auditoria da Aula 17

- `ProtecaoAutenticacaoTest`, `EventoSegurancaTest` e `ProtecaoAutenticacaoPropertiesTest` — 15 testes de domínio e configuração, sem Spring e sem banco;
- `Aula17ProtecaoAuditoriaTest` — integração HTTP com PostgreSQL real e relógio controlado: resposta genérica para login inexistente/senha incorreta/bloqueio, quinta falha, expiração exata, sucesso que limpa o contador, eventos de administração, refresh e logout, consulta paginada/filtrada, 403 para OPERADOR e concorrência com duas transações coordenadas;
- `Aula17MigrationRollbackTest` — cria e remove banco PostgreSQL temporário, aplica 46 changesets, valida constraints e índices, reverte os três changesets da 010 e reaplica;
- `Aula17AuditoriaObrigatoriaTest` — falha na gravação do evento reverte a alteração de status; com a auditoria restaurada, status e evento são confirmados juntos;
- `OpenApiDocumentationTest` — ampliado para verificar `/api/auditoria-seguranca`, `EventoSegurancaResponse`, ausência de `identificadorHash`/segredos e a tag `Auditoria de segurança`.

Nenhum teste usa `Thread.sleep`; o tempo é controlado por `Clock`. Os dados criados são exclusivos e removidos ao final, com limpeza explícita por JDBC. O teste de rollback usa um banco temporário e não apaga `alsports_test`.

### Testes de domínio da Aula 15

`ContaAcessoTest` permanece unitário, sem Spring e sem banco, e agora usa `assertAll` para verificar todos os atributos da conta criada. O cenário de login nulo ou em branco é parametrizado com `@NullAndEmptySource` e `@ValueSource`, cobrindo nulo, vazio, um espaço e múltiplos espaços. O teste `deveAlterarSenhaSomenteQuandoOHashForValido` usa `assertThrows` para garantir que hash em branco ou nulo é rejeitado e que o hash anterior é preservado. A classe executa 20 cenários e pode ser executada isoladamente:

```powershell
.\mvnw.cmd "-Dtest=ContaAcessoTest" test
```

### Testes de aplicação da Aula 15

`ClienteServiceTest` usa Mockito para cobrir as regras reais de `ClienteService` sem iniciar o Spring:

- cadastro para pessoa ativa, com normalização do telefone;
- pessoa inexistente;
- pessoa inativa;
- cliente duplicado para a mesma pessoa;
- busca por identificador e cliente inexistente;
- ordenação inválida e paginação ausente;
- pesquisa com ordenação permitida;
- atualização de telefone e atualização de cliente inexistente.

```powershell
.\mvnw.cmd "-Dtest=ClienteServiceTest" test
```

### Cobertura com JaCoCo

A partir da Aula 15, o `pom.xml` configura o `jacoco-maven-plugin` 0.8.16 com `prepare-agent`, `report` e `check` na fase `verify`. O portão usa a regra `BUNDLE` com cobertura mínima de 70% de linhas e 40% de branches. O agente é injetado via `argLine`, sem interferir no Mockito.

Resultados medidos no AL Sports:

| Momento | Testes | Linhas | Branches | Instruções | Métodos | Classes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline antes dos novos testes | 338 | 84,13% | 69,56% | 82,70% | 84,02% | 94,95% |
| Resultado da Aula 15 | 353 | 85,21% | 71,57% | 84,04% | 85,06% | 94,95% |
| Resultado final da Aula 17 | 425 | 87,03% | 75,07% | 86,27% | 85,57% | 95,69% |

O `ClienteService` passou de 28,6% para 88,6% de linhas e 83,3% de branches. O `ContaAcesso` permanece em 100% de linhas e branches. O `mvn -B clean verify` final da Aula 17 analisou 116 classes e aprovou o portão com 87,03% de linhas e 75,07% de branches, acima dos mínimos de 70% e 40%.

Para executar o ciclo completo com cobertura e portão:

```powershell
.\mvnw.cmd clean verify
```

O relatório HTML é gerado em:

```text
target/site/jacoco/index.html
```

O XML fica em `target/site/jacoco/jacoco.xml` e os dados de execução em `target/jacoco.exec`. A falha controlada do portão foi demonstrada elevando temporariamente o limite de branches para `0.90`: o `jacoco:check` bloqueou o build com os 353 testes aprovados. O limite foi restaurado para `0.40` e o `clean verify` final passou.

A cobertura é interpretada por risco: domínio, segurança e serviços de negócio importam mais do que DTOs e classes de infraestrutura. Classes como `ErroResponse` (sem uso no código de produção), `AdministradorInicialRunner` (execução condicional) e `ColaboradorService` permanecem com lacunas registradas, sem testes artificiais ou exclusões de cobertura.

Para executar todos os testes:

.\mvnw.cmd clean test

## Postman

A collection acumulada para testes manuais da API está em:

docs/postman/AL-Sports-Aula-07.postman_collection.json

O nome da collection foi atualizado para `AL Sports - Aula 17`.

A collection mantém o Bearer Token na raiz:

- `Authorization: Bearer {{accessToken}}`.

A variável `accessToken` é preenchida pelo login do operador. As requisições administrativas usam `{{adminAccessToken}}`, preenchido pelo login do administrador. As rotas públicas (`GET /api/health`, `GET /v3/api-docs` e `POST /api/autenticacao/login`) usam `noauth`. O cadastro de pessoa e conta deixou de ser público e usa o token administrativo.

A pasta `08 - Aula 14` contém 17 requisições:

1. login do administrador e salvamento de `{{adminAccessToken}}`;
2. consulta de `/me` do administrador;
3. criação de pessoa para a conta de operador;
4. criação da conta de operador;
5. promoção da conta a `ADMINISTRADOR`;
6. remoção de `ADMINISTRADOR` da outra conta;
7. bloqueio da conta do operador;
8. reativação da conta do operador;
9. tentativa de bloquear a própria conta, com 400;
10. tentativa de remover o próprio `ADMINISTRADOR`, com 400;
11. lista vazia de perfis, com 400;
12. status ausente, com 400;
13. login do operador e salvamento de `{{accessToken}}`;
14. operador tentando criar conta, com 403;
15. operador tentando excluir categoria, com 403;
16. requisição administrativa sem token, com 401;
17. administrador excluindo categoria inexistente, com 404.

A pasta `07 - Aula 13` mantém o fluxo de login e validação do Bearer. A pasta `06 - Aula 12` mantém health, OpenAPI, consulta da própria conta, alteração de senha e rota protegida sem credencial; as requisições de bootstrap de pessoa e conta agora usam `{{adminAccessToken}}`.

A pasta `11 — Proteção e auditoria da autenticação` contém sete requisições: login inexistente com verificação da mensagem genérica, falha de senha para ser enviada cinco vezes com conta descartável, senha correta durante o bloqueio, consultas administrativas filtradas por `LOGIN_FALHA` e `BLOQUEIO_TEMPORARIO_ATIVADO`, tentativa de OPERADOR (`403`) e consulta sem token (`401`). Os scripts verificam ausência de `identificadorHash`, senha e tokens na resposta. O bloqueio é persistido: não use a conta OPERADOR das outras pastas no experimento.

As variáveis `adminLogin`, `adminPassword`, `authLogin`, `authPassword`, `novaSenha`, `loginInexistente`, `senhaErrada`, `contaDescartavelLogin` e `contaDescartavelSenha` devem ser preenchidas localmente antes da execução. `loginInexistente` e `senhaErrada` são fictícios. A collection não contém credenciais reais, chave JWT ou token válido fixo e não deve ser exportada com valores preenchidos. Access token expira em 30 minutos e refresh token em 30 minutos de inatividade ou 8 horas absolutos; quando receber 401, renove ou repita o login.

A collection também mantém os cenários anteriores:

1. Health Check;
2. criação de categoria;
3. criação de fornecedor;
4. criação de produto;
5. busca de produto;
6. listagem de produtos;
7. produto inválido;
8. produto inexistente;
9. produto duplicado;
10. JSON malformado.

A pasta `05 - Aula 10` contém o fluxo de vendas. Além dos cenários de abertura, inclusão, remoção, finalização e cancelamento, a pasta possui:

- `27 - Venda fracionada`: abre uma venda, inclui o item com `quantidade: 1.500`, valida subtotal e total, finaliza a venda e confere a baixa fracionada no estoque;
- `28 - Quantidade com mais de 3 casas - 400`: envia `quantidade: 1.0001` e valida o retorno HTTP 400 com erro no campo `quantidade`.

Os testes negativos verificam principalmente os códigos:

400 Bad Request
404 Not Found
409 Conflict

## Maven Wrapper

O projeto utiliza o Maven Wrapper.

No Windows:

.\mvnw.cmd clean test

Para compilar:

.\mvnw.cmd clean compile

Para executar a aplicação:

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

## Aulas concluídas

### Aula 00 — Início

Configuração inicial e organização do projeto.

### Aula 01 — Ambiente

Configuração das ferramentas utilizadas no desenvolvimento.

### Aula 02 — Projeto Spring Boot

Criação e configuração inicial do projeto AL Sports com Spring Boot.

### Aula 03 — Domínio

Criação do domínio da aplicação e separação entre domínio e persistência.

### Aula 04 — JPA, PostgreSQL e Liquibase

Integração com PostgreSQL, JPA e Liquibase, incluindo controle das migrations e configuração do ambiente.

### Aula 05 — Repositories, Services e Transações

Implementação dos repositories, regras de aplicação, serviços e operações transacionais.

### Aula 06 — Evolução do modelo e Liquibase

Evolução do modelo de dados, fornecedor, estoque mínimo, constraints e novas migrations.

### Aula 07 — API REST, DTOs, Mapeadores e Postman

Implementação da API REST, DTOs, mapeadores, validações, tratamento de erros, testes com MockMvc e testes manuais utilizando Postman.

### Aula 08 — CRUD, filtros e paginação

Evolução dos endpoints com atualização, exclusão, filtros e paginação.

### Aula 09 — OpenAPI

Documentação do contrato da API com OpenAPI e Swagger UI.

### Aula 10 — Pessoas, vendas e estoque fracionado

Modelagem de pessoas, clientes, colaboradores, vendas e itens de venda, com quantidades decimais e finalização transacional.

### Aula 11 — Concorrência de estoque e locking

Bloqueio pessimista de escrita nas operações que alteram saldo, ordenação determinística dos itens da venda e testes de concorrência com PostgreSQL real.

### Aula 12 — Autenticação, senhas e Spring Security

Identidade de acesso vinculada a `Pessoa`, senhas persistidas com hash BCrypt, autenticação HTTP Basic stateless, endpoints de conta, regras de autorização explícitas, testes de segurança com filtros ativos e documentação OpenAPI do esquema `basicAuth`.

### Aula 13 — JWT e Bearer Token

Migração do HTTP Basic para Bearer Token JWT com OAuth2 Resource Server, assinatura HS256, chave lida de `JWT_SECRET`, validade absoluta de 30 minutos, endpoint público de login, claims mínimas com `sub` e `perfis`, tratamento genérico de credenciais inválidas, testes de emissão e validação, OpenAPI com `bearerAuth` e collection Postman atualizada.

### Aula 14 — Autorização, perfis e controle de acesso

Conversão do claim `perfis` em authorities `ROLE_*`, política HTTP com `OPERADOR` e `ADMINISTRADOR`, defesa em profundidade com `@PreAuthorize`, endpoints administrativos de perfis e status, invariantes do último administrador ativo, provisionamento opcional do administrador inicial e testes de `401`, `403` e `400`.

### Aula 15 — Testes de software, estratégia e cobertura

Organização da suíte como sistema de evidências: níveis de teste, Arrange–Act–Assert, JUnit Jupiter, testes parametrizados, dublês com Mockito, distinção entre testes unitários e de integração, PostgreSQL real, MockMvc com segurança ativa, TDD e limites da cobertura. O `ContaAcessoTest` foi ampliado e o `ClienteServiceTest` foi criado para uma lacuna real. O JaCoCo 0.8.16 mede linhas, branches, instruções, métodos e classes, com portão de 70% de linhas e 40% de branches na fase `verify`.

### Aula 16 — Ciclo de vida dos tokens

Access token JWT stateless e refresh token opaco de 256 bits, persistido apenas como hash SHA-256. Rotação de uso único em famílias com detecção de reutilização e revogação confirmada mesmo em HTTP 401 (`noRollbackFor`), expiração por inatividade e limite absoluto com relógio controlado, `PESSIMISTIC_WRITE` e concorrência real no PostgreSQL, claim `sid`, logout local e global, contratos HTTP genéricos, OpenAPI, Postman e 43 novos testes.

### Aula 17 — Auditoria e proteção da autenticação

Defesa persistente contra tentativas on-line de adivinhar senhas e trilha estruturada de eventos de segurança. Cinco falhas em 15 minutos bloqueiam o login por 15 minutos, com contador correlacionado apenas pelo hash SHA-256 do login normalizado, upsert no PostgreSQL, `PESSIMISTIC_WRITE` e `Clock` controlado. Falhas e recusas usam `REQUIRES_NEW` para sobreviver ao HTTP 401; sucesso e alterações administrativas confirmam ou revertem junto com seus eventos. Doze tipos de evento fechados por enum e `CHECK`, DTO administrativo sem segredos, consulta protegida por perfil na HTTP e por `@PreAuthorize`, migration 010 com rollback explícito, testes isolados de concorrência e rollback, OpenAPI e pasta dedicada no Postman.

Esta aula não implementa WAF, CAPTCHA, MFA, SIEM, proteção distribuída de borda ou não repúdio criptográfico. Os limites, a retenção e o risco de negação de serviço por lockout são registrados como decisões e riscos residuais na documentação específica.

## Status do projeto

**Projeto implementado e verificado até a Aula 17**, com as alterações mantidas na working tree da branch `aula-17`, aguardando autorização para publicação. O marco imutável da Aula 16 permanece no commit `d055813c04a7fd18ac646cf62b331460d98a403e` e na tag `aula-16-refresh-token`.

Principais pontos implementados:

- [x] Projeto Spring Boot configurado
- [x] Java 21
- [x] PostgreSQL
- [x] JPA
- [x] Liquibase
- [x] Domínio separado da persistência
- [x] Categorias
- [x] Produtos
- [x] Fornecedores
- [x] Estoque mínimo
- [x] Quantidades fracionadas com até 3 casas decimais
- [x] Concorrência de estoque com bloqueio pessimista
- [x] Testes de concorrência com PostgreSQL real
- [x] Spring Security como OAuth2 Resource Server
- [x] Bearer Token JWT com assinatura HS256
- [x] Login público com emissão de access token
- [x] Validade absoluta de 30 minutos
- [x] Senhas com hash BCrypt e salt
- [x] Conta de acesso vinculada a Pessoa
- [x] Perfis OPERADOR e ADMINISTRADOR
- [x] Autorização por perfis com `ROLE_*`
- [x] Política HTTP com `denyAll()` como padrão
- [x] `@PreAuthorize` nos casos de uso administrativos
- [x] Endpoints administrativos de perfis e status
- [x] Proteção do último administrador ativo
- [x] Administrador inicial por configuração externa
- [x] Endpoints de conta e troca de senha
- [x] Regras explícitas de rotas públicas e protegidas
- [x] Refresh token opaco com rotação, revogação e logout local/global
- [x] Testes de segurança com filtros ativos
- [x] Testes de emissão e validação de JWT
- [x] Proteção persistente contra falhas de login (5/PT15M/PT15M)
- [x] Bloqueio temporário distinto do bloqueio administrativo
- [x] Trilha estruturada de 12 eventos de segurança
- [x] Auditoria na mesma transação da alteração sensível
- [x] Consulta administrativa paginada e filtrável sem segredos
- [x] Migration 010 com rollback explícito e teste isolado
- [x] OpenAPI com esquema bearerAuth e endpoint de auditoria
- [x] Código de barras
- [x] Repositories
- [x] Services
- [x] Transações
- [x] DTOs
- [x] Mapeadores
- [x] Validações
- [x] Tratamento global de exceções
- [x] API REST
- [x] Testes automatizados
- [x] Estratégia de testes em camadas
- [x] JUnit Jupiter, `assertAll`, `assertThrows` e testes parametrizados
- [x] Dublês de teste com Mockito
- [x] Testes unitários de domínio e aplicação
- [x] Testes de integração com PostgreSQL e Liquibase
- [x] Testes de API e segurança com MockMvc
- [x] Cobertura de código com JaCoCo 0.8.16
- [x] Portão de cobertura de linhas e branches em `verify`
- [x] Relatório HTML de cobertura
- [x] MockMvc
- [x] Testes com Postman
- [x] Configuração por variáveis de ambiente
- [x] Proteção de credenciais
- [x] Documentação do projeto

## Tags das aulas

- `aula-00-inicio`
- `aula-01-ambiente`
- `aula-02-projeto-spring-boot`
- `aula-03-dominio`
- `aula-04-jpa-postgresql-liquibase`
- `aula-05-repositories-servicos-transacoes`
- `aula-06-evolucao-modelo-liquibase-diff`
- `aula-07-api-rest-dtos`
- `aula-07-api-rest-dtos-mappers`
- `aula-08-crud-filtros-paginacao`
- `aula-09-openapi`
- `aula-10-vendas`
- `aula-11-concorrencia`
- `aula-12-autenticacao-http-basic`
- `aula-13-jwt-bearer-token`
- `aula-14-autorizacao-perfis`
- `aula-15-testes-cobertura`
- `aula-16-refresh-token`

A Aula 17 está implementada na branch `aula-17`, sem tag e sem publicação até autorização explícita.

## Documentação

A documentação complementar do tema do projeto está disponível em:

docs/tema-do-projeto.md

A collection do Postman da Aula 07 está disponível em:

docs/postman/AL-Sports-Aula-07.postman_collection.json

A documentação da Aula 11 está disponível em:

docs/11aula/11aula.md

A documentação da Aula 12 está disponível em:

docs/12aula/12aula.md

A documentação da Aula 13 está disponível em:

docs/13aula/13aula.md

A documentação da Aula 14 está disponível em:

docs/14aula/14aula.md

A documentação da Aula 15 está disponível em:

docs/15aula/15aula.md

A documentação da Aula 16 está disponível em:

docs/16aula/16aula.md

A documentação da Aula 17 está disponível em:

docs/17aula/17aula.md

Os diagramas interativos da Aula 14 estão em:

- docs/14aula/diagramas/login-jwt.html
- docs/14aula/diagramas/autorizacao-jwt.html
- docs/14aula/diagramas/administracao-perfis.html
- docs/14aula/diagramas/administrador-inicial.html

## Projeto acadêmico

Projeto desenvolvido para acompanhamento das Aulas 00 a 17 do curso de Java Spring Boot.

**AL Sports — Sistema de controle de loja de artigos esportivos.**
