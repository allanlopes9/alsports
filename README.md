# AL Sports

Sistema para controle de uma loja de artigos esportivos, desenvolvido como projeto acadêmico durante as Aulas 00 a 07 do curso de Java Spring Boot.

## Objetivo

O AL Sports tem como objetivo permitir o cadastro e o gerenciamento de produtos de uma loja de artigos esportivos, organizando os produtos por categoria e fornecedor.

O projeto foi desenvolvido de forma incremental, acompanhando os conceitos apresentados nas Aulas 00 a 07:

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
- testes da API com Postman.

## Tecnologias

- Java 21
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Liquibase
- Maven
- JUnit
- MockMvc
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

A quantidade em estoque não pode ser negativa e o estoque mínimo também possui validação para valores não negativos.

### Categoria

A categoria organiza os produtos da loja.

Uma categoria pode possuir vários produtos, enquanto cada produto pertence a uma categoria.

### Fornecedor

O fornecedor representa a empresa responsável pelo fornecimento dos produtos.

Um produto pode possuir um fornecedor associado.

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
- `FornecedorRepository`.

### service

Contém as regras de aplicação e operações de negócio.

Services atuais:

- `ProdutoService`;
- `CategoriaProdutoService`;
- `FornecedorService`.

As operações de persistência que precisam de transação utilizam `@Transactional`.

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
006-consolidate-categorias.yaml

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
- nome de categoria único, ignorando maiúsculas, minúsculas e espaços nas extremidades.

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
- credenciais;
- arquivos `.env`;
- outros segredos reais.

O `.gitignore` possui a regra:

.env

## API REST

### Health Check

GET /api/health

Resposta:

OK

### Categorias

Criar categoria:

POST /categorias

Listar categorias:

GET /categorias

Buscar categoria:

GET /categorias/{id}

### Fornecedores

Criar fornecedor:

POST /fornecedores

Listar fornecedores:

GET /fornecedores

Buscar fornecedor:

GET /fornecedores/{id}

### Produtos

Criar produto:

POST /produtos

Listar produtos:

GET /produtos

Buscar produto:

GET /produtos/{id}

O projeto também possui operações adicionais de atualização e exclusão de produtos.

## DTO de produto

A criação de produtos utiliza um DTO específico para entrada.

Exemplo:

{
    "codigoBarras": "POSTMAN-BOLA-001",
    "nome": "Bola Postman",
    "quantidade": 10,
    "estoqueMinimo": 2.000,
    "preco": 99.90,
    "dataCadastro": "2026-09-07",
    "ativo": true,
    "unidadeMedida": "UN",
    "categoriaId": 1,
    "fornecedorId": 1
}

O DTO possui validações para garantir que os dados recebidos sejam válidos antes da execução da regra de negócio.

## Validações

Entre as validações implementadas estão:

- código de barras obrigatório;
- código de barras com no máximo 50 caracteres;
- nome obrigatório;
- nome com no máximo 150 caracteres;
- quantidade obrigatória;
- quantidade não negativa;
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
- JSON inválido.

### HTTP 404 — Not Found

Utilizado quando o recurso solicitado não existe.

Exemplo:

GET /produtos/999999999

### HTTP 409 — Conflict

Utilizado quando existe conflito com um recurso já cadastrado.

Exemplos:

- código de barras duplicado;
- nome de produto duplicado;
- CNPJ de fornecedor já cadastrado.

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

Para executar todos os testes:

.\mvnw.cmd clean test

## Postman

A Aula 07 também possui uma collection para testes manuais da API.

Arquivo:

docs/postman/AL-Sports-Aula-07.postman_collection.json

A collection contém cenários de:

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

## Status do projeto

**Projeto concluído até a Aula 07.**

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

## Documentação

A documentação complementar do tema do projeto está disponível em:

docs/tema-do-projeto.md

A collection do Postman da Aula 07 está disponível em:

docs/postman/AL-Sports-Aula-07.postman_collection.json

## Projeto acadêmico

Projeto desenvolvido para acompanhamento das Aulas 00 a 07 do curso de Java Spring Boot.

**AL Sports — Sistema de controle de loja de artigos esportivos.**