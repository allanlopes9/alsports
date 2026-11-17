# Aula 11 — Concorrência de estoque, isolamento transacional e locking

## Problema

Na Aula 10, a finalização de uma venda passou a executar em uma única transação. Se um item não possui saldo, todas as baixas são desfeitas. Isso impede uma venda parcialmente finalizada, mas não impede que duas requisições leiam o mesmo saldo antes de qualquer gravação.

Exemplo: um produto possui `5.000` unidades e duas vendas tentam finalizar `4.000` unidades cada. Sem coordenação, as duas transações podem ler o saldo `5.000`, concluir que existe estoque suficiente e confirmar `8.000` unidades.

A Aula 11 resolve esse cenário com bloqueio pessimista de escrita no banco de dados.

## Conceitos

- **Condição de corrida**: duas transações leem o mesmo dado antes que a primeira grave sua alteração.
- **MVCC**: no PostgreSQL, cada transação enxerga um instantâneo consistente dos dados; leituras não bloqueiam gravações.
- **Isolamento transacional**: define quais alterações de uma transação concorrente são visíveis para outra.
- **Bloqueio otimista**: detecta o conflito no momento da gravação, normalmente com versão do registro.
- **Bloqueio pessimista**: impede o acesso concorrente ao registro antes da alteração.
- **Deadlock**: duas transações aguardam recursos mantidos uma pela outra.

## Implementação

### Finalização de venda

`VendaService.finalizar` mantém `@Transactional`, ordena os itens por `produto.id` crescente e adquire `PESSIMISTIC_WRITE` antes de validar o produto e baixar o estoque:

```java
@Transactional
public Venda finalizar(Long id) {
    Venda venda = buscarEntidade(id);

    validarParticipantes(venda.getCliente(), venda.getVendedor());

    List<ItemVenda> itensOrdenados = venda.getItens().stream()
            .sorted(Comparator.comparing(
                    item -> item.getProduto().getId()))
            .toList();

    for (ItemVenda item : itensOrdenados) {
        Produto produto = item.getProduto();

        entityManager.refresh(
                produto,
                LockModeType.PESSIMISTIC_WRITE);

        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new IllegalArgumentException(
                    "Todos os produtos da venda devem estar ativos");
        }

        produto.retirarEstoque(item.getQuantidade());
    }

    venda.finalizar();

    return repository.saveAndFlush(venda);
}
```

Pontos importantes:

1. O `refresh` com `PESSIMISTIC_WRITE` recarrega o produto e adquire o bloqueio de escrita.
2. A validação de produto ativo e a retirada de estoque ocorrem depois do bloqueio, usando o saldo atual do banco.
3. A ordenação por identificador é aplicada antes do primeiro bloqueio, independentemente da ordem dos itens na venda.
4. A venda só muda para `FINALIZADA` após todos os itens serem processados.
5. Qualquer exceção desfaz a transação inteira, incluindo baixas já realizadas em memória.

### Entrada e saída manual

`ProdutoService.receberEstoque` e `ProdutoService.retirarEstoque` também adquirem o bloqueio antes de alterar o saldo:

```java
@Transactional
public Produto retirarEstoque(Long id, BigDecimal quantidade) {
    Produto produto = buscarEntidade(id);
    entityManager.refresh(
        produto,
        LockModeType.PESSIMISTIC_WRITE);
    produto.retirarEstoque(quantidade);
    return produto;
}
```

Consultas, listagens e alterações de preço ou descrição continuam sem bloqueio. O método `buscarPorId` apenas delega para `buscarEntidade`, sem `refresh` com lock.

### Ordenação determinística

Duas vendas que compartilham os mesmos produtos podem adquirir os bloqueios em ordens opostas se seguirem a ordem dos itens. A ordenação por `produto.id` garante que todas as transações solicitem os bloqueios na mesma ordem, reduzindo o risco de deadlock.

A ordenação não substitui o timeout do banco nem o tratamento de deadlock; ela apenas elimina a principal causa evitável no fluxo da venda.

## Teste de concorrência

O teste real está em:

```
src/test/java/com/curso/alsports/Aula11ConcorrenciaTests.java
```

A classe usa `@SpringBootTest` com o profile `test`, mas **não** usa `@Transactional`. Isso é essencial: se o teste abrisse uma transação, as chamadas aos serviços participariam da mesma transação e não haveria concorrência real.

Cada tarefa é submetida a um `ExecutorService` e aguarda um `CountDownLatch` para iniciar ao mesmo tempo. Os `Future` possuem timeout de 30 segundos. As chamadas são feitas aos beans injetados, portanto passam pelo proxy do Spring e abrem transações independentes.

O saldo e o status são conferidos por `JdbcTemplate`, em consultas fora das transações dos serviços.

### Cenários cobertos

| Teste | Verificação |
| --- | --- |
| `devePermitirApenasUmaFinalizacaoQuandoDuasVendasDisputamOMesmoSaldo` | Produto com `5.000`, duas vendas de `4.000`: exatamente 1 sucesso e 1 falha por saldo, saldo final `1.000`, 1 venda `FINALIZADA` e 1 `ABERTA`. |
| `deveCoordenarVendaERetiraManualConcorrentes` | Venda e retirada manual de `4.000` disputam o mesmo saldo: apenas uma operação conclui e o saldo não fica negativo. |
| `deveCoordenarVendaEEntradaManualConcorrentes` | Venda de `4.000` e entrada manual de `4.000`: as duas concluem e o saldo final é `5.000`. |
| `deveDesfazerBaixaDeTodosOsItensQuandoUmProdutoNaoPossuirSaldo` | Venda com dois itens, sendo o segundo sem saldo: nenhum produto é alterado e a venda permanece `ABERTA`. |
| `deveProcessarProdutosEmOrdemCrescenteSemDeadlock` | Duas vendas incluem os mesmos produtos em ordens opostas: ambas finalizam e o saldo de cada produto cai de `10.000` para `8.000`. |
| `deveRespeitarSaldoFracionadoEmFinalizacoesConcorrentes` | Produto com `2.000`, duas vendas de `1.500`: apenas uma finaliza e o saldo final é `0.500`. |

A mensagem esperada para falta de saldo é:

```
Quantidade para retirada maior que o estoque disponível
```

### Limpeza

Cada teste cria seus próprios participantes, produtos e categorias e remove os registros no bloco `finally`. A verificação de resíduos compara as contagens antes e depois de uma execução isolada da classe; as contagens permanecem idênticas.

## Testes unitários

Os testes de serviço usam Mockito e complementam os testes concorrentes:

- `src/test/java/com/curso/alsports/service/ProdutoServiceTest.java` verifica o lock em entrada e saída, a ausência de lock em consulta e a preservação do saldo quando a retirada excede o disponível.
- `src/test/java/com/curso/alsports/service/VendaServiceTest.java` verifica a ordem crescente dos locks, o lock antes da baixa, o saldo insuficiente, o produto inativo e a interrupção no segundo item sem saldo.

## Evidências da execução

Execução em PostgreSQL 16.13, banco `alsports_test`, profile `test`:

- suíte anterior à Aula 11: 158 testes, 0 falhas, 0 erros, 0 ignorados;
- suíte após a Aula 11: 173 testes, 0 falhas, 0 erros, 0 ignorados, em 16 classes;
- `Aula11ConcorrenciaTests`: 6 testes, 0 falhas;
- `ProdutoServiceTest`: 4 testes, 0 falhas;
- `VendaServiceTest`: 5 testes, 0 falhas;
- nenhuma migration criada ou alterada; `db.changelog-master.yaml` permaneceu inalterado.

## Como executar

Com o banco de teste configurado e as variáveis `DB_TEST_URL`, `DB_TEST_USERNAME` e `DB_TEST_PASSWORD` disponíveis:

```
.\mvnw.cmd test
```

Para executar apenas os testes concorrentes:

```
.\mvnw.cmd "-Dtest=Aula11ConcorrenciaTests" test
```

## Fora do escopo

Esta aula não implementa:

- reserva de estoque na abertura da venda;
- bloqueio distribuído;
- retry automático em caso de deadlock;
- controle de versão com `@Version`;
- `synchronized` ou locks em memória;
- alterações de schema;
- autenticação e autorização com Security/JWT;
- processamento assíncrono por filas.

## Resumo

A concorrência é tratada no banco de dados, dentro da transação, com bloqueio pessimista de escrita em todas as operações que alteram saldo. A ordenação por identificador do produto torna a aquisição dos locks previsível. Os testes executam transações reais e simultâneas em PostgreSQL, comprovando que o sistema não confirma vendas acima do estoque disponível, não deixa saldo negativo e desfaz a venda inteira quando um item falha.
