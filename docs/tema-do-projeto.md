# AL Sports

## Identificação

- Nome do projeto: AL Sports
- Tema: controle de uma loja de artigos esportivos
- Objetivo em uma frase: cadastrar e gerenciar produtos esportivos, organizando-os por categoria e fornecedor.

## Entidade de classificação

- Nome no singular: CategoriaProduto
- Nome no plural: CategoriasProduto
- Exemplo 1: Camisas
- Exemplo 2: Calçados
- Exemplo 3: Bolas

## Entidade principal

- Nome no singular: Produto
- Nome no plural: Produtos
- Código único: Código de barras
- Descrição: Nome do produto
- Medida quantitativa: Quantidade em estoque
- Estoque mínimo: Quantidade mínima recomendada em estoque
- Valor monetário: Preço de venda
- Data relevante: Data de cadastro
- Status: Ativo ou inativo
- Unidade de medida: Unidade utilizada para controle do estoque
- Categoria: Categoria à qual o produto pertence
- Fornecedor: Fornecedor responsável pelo produto

## Relacionamentos

- Uma categoria pode possuir vários produtos.
- Cada produto pertence a uma categoria.
- Um fornecedor pode fornecer vários produtos.
- Um produto pode possuir um fornecedor associado.

## Regras principais

- O código de barras é obrigatório e único.
- O nome do produto é obrigatório e único.
- A quantidade em estoque não pode ser negativa.
- O estoque mínimo não pode ser negativo.
- O preço não pode ser negativo.
- Toda categoria informada deve existir.
- O fornecedor é opcional, mas quando informado deve existir.
- Produtos podem estar ativos ou inativos.

## Exemplos de produtos

- Camiseta esportiva
- Shorts
- Tênis
- Bola
- Meia
- Acessórios esportivos

## Evolução do projeto

O projeto foi desenvolvido de forma incremental durante as Aulas 00 a 07 do curso de Java Spring Boot, passando por:

- configuração do ambiente;
- criação do projeto Spring Boot;
- modelagem do domínio;
- persistência com JPA;
- PostgreSQL;
- Liquibase;
- repositories;
- services;
- transações;
- evolução do modelo;
- DTOs;
- mapeadores;
- API REST;
- validação;
- tratamento global de erros;
- testes automatizados;
- testes da API com Postman.