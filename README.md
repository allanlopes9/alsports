# AL Sports

Sistema para controle de uma loja de artigos esportivos, desenvolvido como projeto acadêmico durante as Aulas 00 a 07.

## Objetivo

Permitir o cadastro e o gerenciamento de produtos esportivos, organizando-os por categoria.

## Tecnologias

* Java 21
* Spring Boot 4.0.7
* Spring Web
* Spring Data JPA
* PostgreSQL
* Liquibase
* Maven
* JUnit
* MockMvc

## Domínio

### Produto

A entidade principal do sistema representa os produtos esportivos, contendo informações como:

* Nome
* Quantidade em estoque
* Estoque mínimo
* Preço
* Data de cadastro
* Status ativo/inativo
* Unidade de medida
* Categoria
* Fornecedor

### Categoria

Uma categoria pode possuir vários produtos, enquanto cada produto pertence a uma categoria.

## Estrutura

O projeto está organizado em camadas:

* `api` — endpoints básicos da aplicação
* `controller` — controladores REST
* `dto` — objetos de entrada e saída da API
* `exception` — tratamento de erros
* `model` — entidades do domínio
* `repository` — acesso aos dados
* `service` — regras de aplicação
* `resources/db/changelog` — migrations do Liquibase

## Requisitos

* JDK 21
* PostgreSQL
* Git

O projeto utiliza o Maven Wrapper, portanto não é necessário instalar o Maven separadamente.

## Configuração do banco

As configurações de banco são fornecidas por variáveis de ambiente.

Utilize o arquivo `.env.example` como referência e crie seu `.env` local.

O arquivo `.env` contém informações sensíveis e não deve ser versionado.

## Executando o projeto

No Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Para executar os testes:

```powershell
.\mvnw.cmd clean test
```

## API

Endpoint de saúde:

```text
GET /api/health
```

Resposta esperada:

```text
OK
```

Os endpoints de produtos e categorias estão disponíveis nas respectivas rotas REST da aplicação.

## Banco de dados

As alterações do banco são controladas pelo Liquibase.

O projeto utiliza `ddl-auto=validate`, deixando a evolução do schema sob responsabilidade das migrations.

## Segurança

Nunca versione:

* senhas;
* tokens;
* chaves privadas;
* arquivos `.env`;
* outras credenciais reais.

O arquivo `.env.example` contém apenas valores de exemplo e placeholders.

## Testes

Execute:

```powershell
.\mvnw.cmd test
```

Os testes automatizados validam funcionalidades do domínio, persistência, transações e API REST.

## Projeto acadêmico

Projeto desenvolvido para acompanhamento das Aulas 00 a 07.

### Tags

* `aula-00-inicio`
* `aula-01-ambiente`
* `aula-02-projeto-spring-boot`
* `aula-03-dominio`
* `aula-04-jpa-postgresql-liquibase`
* `aula-05-repositories-servicos-transacoes`
* `aula-06-evolucao-modelo-liquibase-diff`
* `aula-07-api-rest-dtos`

## Documentação

A documentação do tema do projeto está disponível em:

`docs/tema-do-projeto.md`
