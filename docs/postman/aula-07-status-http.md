# Aula 07 — Status HTTP

Documentação dos principais códigos HTTP utilizados pela API REST do AL Sports.

## HTTP 200 — OK

Utilizado quando uma consulta é realizada com sucesso.

Exemplos:

- GET /categorias
- GET /categorias/{id}
- GET /fornecedores
- GET /fornecedores/{id}
- GET /produtos
- GET /produtos/{id}

## HTTP 201 — Created

Utilizado quando um novo recurso é criado com sucesso.

Exemplos:

- POST /categorias
- POST /fornecedores
- POST /produtos

## HTTP 400 — Bad Request

Utilizado quando a requisição contém dados inválidos ou está malformada.

Exemplos testados no Postman:

- produto com campos obrigatórios inválidos;
- código de barras vazio;
- quantidade negativa;
- preço inválido;
- categoria inválida;
- JSON malformado.

## HTTP 404 — Not Found

Utilizado quando o recurso solicitado não existe.

Exemplo testado:

GET /produtos/999999999

Resposta:

- status: 404
- erro: Recurso não encontrado

## HTTP 409 — Conflict

Utilizado quando a operação entra em conflito com um recurso já cadastrado.

Exemplo testado no Postman:

- criação de produto utilizando um código de barras já existente.

O AL Sports utiliza o código de barras como identificador único do produto. Por isso, uma tentativa de cadastrar outro produto com o mesmo código de barras resulta em HTTP 409.

Exemplo:

POST /produtos

Código de barras já existente:

POSTMAN-BOLA-001

Resposta:

- status: 409
- erro: Recurso duplicado

## HTTP 500 — Internal Server Error

Representa um erro inesperado da aplicação.

A API possui tratamento global de exceções para evitar que detalhes internos sejam expostos ao cliente.

Não devem ser retornados pela API:

- stack traces;
- senhas;
- tokens;
- credenciais;
- comandos SQL;
- detalhes internos da infraestrutura.

## Cenários testados

A collection do Postman da Aula 07 contém os seguintes cenários:

1. Health Check;
2. criação de categoria;
3. criação de fornecedor;
4. criação de produto;
5. busca de produto;
6. listagem de produtos;
7. produto inválido — 400;
8. produto inexistente — 404;
9. produto duplicado por código de barras — 409;
10. JSON malformado — 400.

## Collection

Arquivo utilizado:

docs/postman/AL-Sports-Aula-07.postman_collection.json

A collection está organizada para executar primeiro os cadastros necessários e depois os testes de consulta e cenários de erro.