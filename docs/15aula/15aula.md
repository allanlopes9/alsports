# Aula 15 — Testes de software, estratégia de testes e cobertura com JaCoCo

## Apresentação

Nas aulas anteriores o AL Sports acumulou domínio, persistência com PostgreSQL e Liquibase, serviços transacionais, API REST, concorrência, autenticação JWT e autorização por perfis. Cada incremento trouxe testes, mas a suíte nunca foi analisada como um conjunto organizado de evidências.

Esta aula responde à pergunta:

> Como obter evidências de que regras de negócio, integrações, contratos HTTP e controles de segurança continuam corretos depois de uma mudança?

Em vez de criar exemplos artificiais, a aula investiga os testes reais do projeto, classifica-os por nível e mede quais partes do código de produção eles exercitam com o JaCoCo.

O resultado é uma estratégia em camadas:

```text
muitos testes rápidos de domínio e aplicação
        +
testes de integração seletivos com PostgreSQL
        +
testes de API, contrato e segurança
        +
relatório de cobertura interpretado por risco
```

### Problema orientador

> Um build verde garante que o sistema está correto?

Não. Um build verde garante apenas que as verificações existentes passaram. A qualidade da conclusão depende de quais comportamentos foram verificados, com quais asserções e em qual nível de integração.

### Escopo

Nesta aula:

- diferenciamos verificação e validação;
- classificamos a suíte existente por nível de teste;
- revisamos a qualidade das asserções e o isolamento dos testes;
- aprimoramos `ContaAcessoTest` com `assertAll`, `assertThrows` e teste parametrizado;
- criamos `ClienteServiceTest` para uma lacuna real de cobertura;
- configuramos o JaCoCo 0.8.16 com `prepare-agent`, `report` e `check`;
- medimos a cobertura antes e depois das mudanças;
- demonstramos uma falha controlada do portão de cobertura;
- documentamos resultados reais, limites e decisões.

Ainda não fazem parte do escopo:

- testes de mutação;
- testes de contrato entre serviços externos;
- testes de carga e desempenho;
- cobertura de front-end;
- pipeline de integração contínua.

## Resultados de aprendizagem

Ao final desta aula, você deve ser capaz de:

- diferenciar verificação de validação;
- reconhecer os níveis de teste e o que cada um evidencia;
- estruturar testes no padrão Arrange–Act–Assert;
- usar JUnit Jupiter, `assertAll`, `assertThrows` e testes parametrizados;
- decidir quando usar objetos reais, stubs ou mocks;
- distinguir teste unitário de teste de integração;
- reconhecer o papel do PostgreSQL real nos testes de persistência;
- interpretar cobertura de linhas, branches, instruções, métodos e classes;
- explicar por que cobertura alta não garante qualidade;
- configurar o JaCoCo e um portão de cobertura no Maven;
- interpretar uma falha de portão de cobertura;
- documentar resultados sem inflar percentuais.

## Pré-requisitos

- projeto da Aula 14 compilando e com os testes aprovados;
- PostgreSQL disponível para o profile `test`;
- Java 21;
- Maven Wrapper;
- `.env` local configurado e ignorado pelo Git;
- compreensão das entidades, services, API e da segurança das Aulas 10 a 14.

Confirme o ponto inicial:

```powershell
git status
git log -1 --oneline
.\mvnw.cmd test
```

O ponto de partida deve estar no marco `aula-14-autorizacao-perfis`. Não continue se houver alterações desconhecidas ou falhas anteriores.

## 1. Verificação e validação

### 1.1 Definições

**Verificação** responde:

> Estamos construindo o produto corretamente?

É a conferência de artefatos contra especificações: compila, respeita o contrato, passa nas asserções, não viola constraints.

**Validação** responde:

> Estamos construindo o produto correto?

É a conferência do produto contra a necessidade real: a regra de negócio faz sentido, o fluxo atende ao usuário, a decisão de segurança é adequada.

Um teste automatizado é uma ferramenta de verificação. Ele só contribui para a validação quando a asserção representa um comportamento relevante para o negócio.

### 1.2 O que um teste verde não prova

Um teste verde não prova que:

- o requisito foi interpretado corretamente;
- todos os caminhos foram exercitados;
- o código não tem efeitos colaterais fora do cenário testado;
- a integração real com o banco, a rede ou o provedor de identidade funciona;
- o comportamento é estável sob concorrência;
- a cobertura de linhas corresponde a cobertura de decisões.

Ele prova que aquele cenário específico, com aqueles dados e aquelas asserções, produziu o resultado esperado.

## 2. Níveis de teste

A suíte do AL Sports não é homogênea. Cada classe responde a uma pergunta diferente e exige infraestrutura diferente.

| Nível | Pergunta | Exemplo no AL Sports | Infraestrutura |
| --- | --- | --- | --- |
| Unitário de domínio | A regra da entidade está correta? | `model.ContaAcessoTest`, `model.PessoaTest` | Nenhuma; objetos reais |
| Unitário de aplicação | O service coordena as regras e dependências corretamente? | `service.ClienteServiceTest`, `service.ContaAcessoServiceTest` | Mockito |
| Integração de aplicação | O contexto Spring sobe e os beans colaboram? | `AlsportsApplicationTests` | Spring Boot |
| Integração de persistência | O mapeamento, o schema e as constraints funcionam? | `Aula10IntegracaoTests` | Spring Boot + PostgreSQL + Liquibase |
| Componente/API | O contrato HTTP responde como esperado? | `AlsportsApplicationTests`, `Aula10IntegracaoTests` | MockMvc |
| Contrato/documentação | O contrato OpenAPI continua válido? | `domain.OpenApiDocumentationTest` | Spring Boot + MockMvc |
| Segurança | Autenticação e autorização decidem corretamente? | `AutenticacaoSecurityTest`, `AutorizacaoSecurityTest`, `ContaAcessoSecurityTest` | MockMvc com filtros ativos |
| Concorrência | O locking preserva o saldo sob disputa? | `Aula11ConcorrenciaTests` | PostgreSQL real + threads |

### 2.1 Pirâmide e custo

A distribuição do AL Sports segue uma pirâmide prática:

- muitos testes de domínio e de aplicação, rápidos e sem infraestrutura;
- integrações seletivas com PostgreSQL para schema, constraints e transações;
- testes de API e segurança em quantidade menor, porém com maior custo;
- testes de concorrência em número reduzido, pois são os mais lentos e sensíveis.

O custo cresce com a infraestrutura. Um teste de domínio executa em milissegundos; um teste de concorrência com PostgreSQL pode levar segundos e depender do estado do banco.

### 2.2 Evidência e limite de cada nível

- Domínio: evidencia regras puras; não evidencia persistência nem integração.
- Aplicação: evidencia coordenação e tratamento de exceções; não evidencia SQL nem constraints.
- Integração: evidencia schema, mapeamento e transação; não evidencia decisões de autorização.
- API: evidencia status, corpo e validação; não evidencia regras internas isoladas.
- Segurança: evidencia `401`, `403` e política de rotas; não evidencia a regra de negócio.
- Concorrência: evidencia locking e rollback; não evidencia usabilidade nem contrato.

## 3. Arrange–Act–Assert

O padrão AAA separa três momentos:

1. **Arrange** — prepara objetos, dados e dublês;
2. **Act** — executa exatamente a operação observada;
3. **Assert** — verifica resultados observáveis.

Exemplo real de `ContaAcessoTest`:

```java
@Test
void deveCriarContaAtivaComSomentePerfilOperador() {
    // Arrange
    ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

    // Act + Assert agrupados
    assertAll(
            () -> assertThat(conta.getPessoa()).isSameAs(pessoa),
            () -> assertThat(conta.getLogin()).isEqualTo("ana.souza"),
            () -> assertThat(conta.getSenhaHash()).isEqualTo(HASH),
            () -> assertThat(conta.getStatus())
                    .isEqualTo(StatusContaAcesso.ATIVA),
            () -> assertThat(conta.getPerfis())
                    .containsExactly(PerfilAcesso.OPERADOR),
            () -> assertThat(conta.podeAutenticar()).isTrue());
}
```

Vantagens do padrão:

- o leitor identifica rapidamente o que está sendo testado;
- o teste não mistura preparação com verificação;
- falhas ficam localizadas em um único bloco.

### 3.1 Nomes que descrevem comportamento

Os nomes seguem a forma `deve...` ou `naoDeve...` e descrevem o comportamento, não o método:

- `deveApararEspacosExternosDoLogin`;
- `naoDeveAutenticarContaBloqueada`;
- `naoDeveCadastrarClienteDuplicadoParaAMesmaPessoa`.

Um nome ruim, como `testeLogin1`, não informa qual regra falhou quando o teste quebra.

## 4. JUnit Jupiter

O projeto usa JUnit Jupiter, integrado pelo `spring-boot-starter-test`. Os recursos usados nesta aula:

- `@Test` — identifica um caso;
- `@BeforeEach` — prepara o estado comum;
- `assertAll` — agrupa asserções e executa todas, mesmo que uma falhe;
- `assertThrows` — verifica a exceção esperada;
- `@ParameterizedTest` — executa o mesmo corpo com entradas diferentes;
- `@NullAndEmptySource` e `@ValueSource` — fornecem entradas parametrizadas;
- `@ExtendWith(MockitoExtension.class)` — integra Mockito ao ciclo de vida do JUnit.

### 4.1 `assertAll`

Sem `assertAll`, a primeira asserção que falha interrompe o teste e esconde as demais. Com `assertAll`, todas são avaliadas e o relatório mostra todos os problemas de uma vez.

### 4.2 `assertThrows`

```java
IllegalArgumentException erro = assertThrows(
        IllegalArgumentException.class,
        () -> new ContaAcesso(pessoa, loginInvalido, HASH));

assertEquals("Login é obrigatório", erro.getMessage());
```

O teste verifica não só o tipo da exceção, mas também a mensagem estável que o contrato expõe.

### 4.3 Testes parametrizados

O teste de login inválido cobre quatro entradas:

```java
@ParameterizedTest(name = "login inválido: [{0}]")
@NullAndEmptySource
@ValueSource(strings = {" ", "   "})
void naoDeveAceitarLoginNuloOuEmBranco(String loginInvalido) {
    IllegalArgumentException erro = assertThrows(
            IllegalArgumentException.class,
            () -> new ContaAcesso(pessoa, loginInvalido, HASH));

    assertEquals("Login é obrigatório", erro.getMessage());
}
```

Uma única regra, quatro execuções registradas pelo JUnit. Isso evita copiar e colar o mesmo teste quatro vezes, sem esconder cenários.

## 5. Dublês de teste e Mockito

Um dublê substitui uma dependência real para isolar o objeto observado.

- **Stub** — devolve respostas programadas.
- **Mock** — permite verificar interações.
- **Fake** — implementação simplificada, porém funcional.

No AL Sports, os testes de aplicação usam Mockito para substituir repositories:

```java
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @Mock
    private PessoaRepository pessoaRepository;

    private ClienteService service;
    // ...
}
```

### 5.1 Quando usar mock

Use mock quando:

- a dependência é externa, lenta ou não determinística;
- o objetivo é verificar coordenação e tratamento de exceções;
- a dependência real exigiria infraestrutura desnecessária.

Não use mock quando:

- a dependência é simples e determinística;
- o teste precisa validar o comportamento real do banco;
- o mock apenas replica a implementação e não verifica comportamento observável.

### 5.2 Excesso de verificação de interações

Verificar cada chamada interna produz testes frágeis: qualquer refatoração sem mudança de comportamento quebra a suíte. Prefira verificar resultados observáveis e use `verify` apenas quando a interação for parte do contrato.

### 5.3 Unitário versus integração

O mesmo service pode ser testado em dois níveis:

- `ClienteServiceTest` usa Mockito para verificar regras de aplicação sem banco;
- `Aula10IntegracaoTests` usa Spring e PostgreSQL para verificar persistência e transações.

Os dois níveis não são redundantes: um isola a decisão, o outro valida a integração real.

## 6. PostgreSQL e Liquibase nos testes

O profile `test` aponta exclusivamente para o banco de testes:

```properties
spring.datasource.url=${DB_TEST_URL:jdbc:postgresql://localhost:5432/alsports_test}
spring.datasource.username=${DB_TEST_USERNAME:alsports_app}
spring.datasource.password=${DB_TEST_PASSWORD}
```

O banco de testes não é o banco de desenvolvimento nem o de produção. O Liquibase aplica as migrations no banco de teste, e o Hibernate valida o schema com `ddl-auto=validate`.

### 6.1 Por que não usar H2

H2 não reproduz fielmente:

- tipos `NUMERIC(18,3)`;
- constraints e índices únicos;
- `ON DELETE RESTRICT` e `ON DELETE CASCADE`;
- locking pessimista;
- comportamento transacional do PostgreSQL.

Um teste que passa no H2 pode falhar no PostgreSQL real. Por isso a suíte de integração mantém o PostgreSQL.

### 6.2 `flush()` e constraints

Para validar constraints do banco antes do fim da transação, alguns testes chamam `flush()`. Sem o flush, o Hibernate pode adiar o `INSERT` e a violação só aparecer no commit, fora do trecho observado.

### 6.3 Credencial de teste

O projeto exige `DB_TEST_PASSWORD` no processo. Nesta execução, a credencial foi disponibilizada ao Spring por configuração externa, sem leitura, impressão ou registro do valor. Nenhum segredo foi copiado para a documentação, para os testes ou para o repositório.

## 7. MockMvc e segurança

Os testes de API usam MockMvc. A cadeia de filtros do Spring Security permanece ativa nas classes de segurança:

- `AutenticacaoSecurityTest`;
- `AutorizacaoSecurityTest`;
- `ContaAcessoSecurityTest`;
- `OpenApiDocumentationTest`.

Os demais testes de API usam `@AutoConfigureMockMvc(addFilters = false)` para focar no contrato, sem repetir a autenticação em todos os cenários.

### 7.1 O que é verificado

- rotas públicas respondem sem token;
- rotas protegidas respondem `401` sem token;
- operador recebe `403` em rota administrativa;
- administrador executa a operação;
- token inválido, expirado ou adulterado recebe `401`;
- respostas não expõem senha, hash ou stack trace.

### 7.2 O que não é verificado

MockMvc não substitui um teste de carga nem valida TLS. Ele exercita a cadeia de filtros e o contrato HTTP dentro da JVM.

## 8. TDD

O ciclo TDD é:

1. **Red** — escrever um teste que falha pelo motivo correto;
2. **Green** — implementar o mínimo para passar;
3. **Refactor** — melhorar o código mantendo o teste verde.

### 8.1 O que foi feito nesta aula

Os testes novos foram escritos contra comportamentos já implementados e corretos. Nesse caso, o teste nasce verde: é um teste de caracterização, não um ciclo Red–Green–Refactor completo.

Não foi criada uma falha artificial no código de produção apenas para simular o ciclo. A evidência real de **Red** nesta aula é a falha controlada do portão de cobertura, descrita na seção 11: o build falha porque a cobertura de branches medida é inferior ao limite temporário.

### 8.2 Quando o TDD é mais valioso

O TDD é mais valioso quando:

- a regra ainda não existe;
- o comportamento é ambíguo e precisa ser especificado;
- a mudança é pequena e o teste pode ser escrito antes.

Em código legado já correto, o teste de caracterização protege contra regressões, mas não substitui a especificação.

## 9. JaCoCo

O JaCoCo mede a execução do código de produção durante os testes.

### 9.1 Configuração no `pom.xml`

Propriedades:

```xml
<jacoco.version>0.8.16</jacoco.version>
<jacoco.minimum.line.coverage>0.70</jacoco.minimum.line.coverage>
<jacoco.minimum.branch.coverage>0.40</jacoco.minimum.branch.coverage>
```

Plugin:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <executions>
        <execution>
            <id>prepare-jacoco-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report-and-check-coverage</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>${jacoco.minimum.line.coverage}</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>${jacoco.minimum.branch.coverage}</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 9.2 `prepare-agent` e `argLine`

O `prepare-agent` injeta o agente Java nos testes e define `argLine`:

```text
-javaagent:...org.jacoco.agent-0.8.16-runtime.jar=destfile=...\target\jacoco.exec
```

O plugin não sobrescreve o `argLine` do Surefire de forma destrutiva: o agente é adicionado antes da execução. O Mockito continua funcionando; nesta execução o Mockito apenas emitiu o aviso padrão de auto-attach do `inline-mock-maker`, sem falha.

### 9.3 Métricas

- **INSTRUCTION** — instruções de bytecode executadas;
- **BRANCH** — decisões (`if`, `switch`, operadores lógicos) exercitadas;
- **LINE** — linhas de código executadas;
- **METHOD** — métodos executados;
- **CLASS** — classes com ao menos um método executado;
- **COMPLEXITY** — complexidade ciclomática coberta.

### 9.4 Limites do indicador

Cobertura mede execução, não verificação:

- uma linha pode ser executada sem que o resultado seja verificado;
- um `if` pode ser coberto sem que o caso de negócio seja relevante;
- código morto executado por engano aumenta o percentual;
- branches podem ser cobertos por testes frágeis;
- cobertura alta não garante ausência de regressão.

Por isso a leitura é por risco: classes de domínio e segurança importam mais do que DTOs e classes de infraestrutura.

## 10. Resultados reais do AL Sports

### 10.1 Baseline antes dos novos testes

A baseline foi executada com o JaCoCo já configurado, mas **antes** de qualquer teste novo:

| Métrica | Cobertos | Total | Percentual |
| --- | ---: | ---: | ---: |
| Linhas | 1633 | 1941 | 84,13% |
| Branches | 345 | 496 | 69,56% |
| Instruções | 5694 | 6885 | 82,70% |
| Métodos | 568 | 676 | 84,02% |
| Classes | 94 | 99 | 94,95% |

Suíte: 338 testes, 0 falhas, 0 erros, 0 ignorados, 27 classes, aproximadamente 35,4 s.

### 10.2 Cobertura por pacote na baseline

| Pacote | Linhas | Branches |
| --- | ---: | ---: |
| `controller` | 74,7% | 71,4% |
| `service` | 75,0% | 60,1% |
| `repository` | 81,1% | 79,2% |
| `model` | 97,5% | 85,1% |
| `config` | 87,8% | 87,5% |
| `domain` | 84,2% | 74,1% |
| `dto` | 92,7% | 57,1% |
| `exception` | 71,7% | n/a |
| `validation` | 100% | 88,9% |

### 10.3 Lacunas identificadas

- `ClienteService` com 28,6% de linhas, sem teste dedicado, apesar de conter regras reais (pessoa ativa, duplicidade, ordenação permitida);
- `ColaboradorService` com 28% de linhas;
- `PessoaService` com 42,6% de linhas;
- `ErroResponse` com 0% e sem uso no código de produção — código morto, não recebeu teste artificial;
- `AdministradorInicialRunner` com 0% — execução condicional por configuração externa, registrado como lacuna;
- `CategoriaProdutoRequest` e `ColaboradorAtualizacaoRequest` com 0% — DTOs de entrada sem fluxo exercitado.

### 10.4 Resultado final

Após o aprimoramento de `ContaAcessoTest` e a criação de `ClienteServiceTest`:

| Métrica | Cobertos | Total | Percentual |
| --- | ---: | ---: | ---: |
| Linhas | 1654 | 1941 | 85,21% |
| Branches | 355 | 496 | 71,57% |
| Instruções | 5786 | 6885 | 84,04% |
| Métodos | 575 | 676 | 85,06% |
| Classes | 94 | 99 | 94,95% |

Suíte final: 353 testes, 0 falhas, 0 erros, 0 ignorados, 28 classes.

### 10.5 Cobertura por pacote no resultado final

| Pacote | Linhas | Branches |
| --- | ---: | ---: |
| `controller` | 74,7% | 71,4% |
| `service` | 78,8% | 63,2% |
| `repository` | 81,1% | 79,2% |
| `model` | 97,5% | 87,2% |
| `config` | 87,8% | 87,5% |
| `domain` | 84,2% | 74,1% |
| `dto` | 92,7% | 57,1% |
| `exception` | 71,7% | n/a |
| `validation` | 100% | 88,9% |

### 10.6 Classes de interesse

| Classe | Linhas | Branches |
| --- | ---: | ---: |
| `ClienteService` | 88,6% (31/35) | 83,3% (10/12) |
| `ContaAcesso` | 100% (45/45) | 100% (26/26) |
| `Cliente` | 100% (12/12) | 100% (4/4) |

O `ClienteService` subiu de 28,6% para 88,6% de linhas com 11 testes de aplicação. O `ContaAcesso` permaneceu em 100%, agora com asserções agrupadas, cenários parametrizados e verificação de `alterarSenha` inválido.

### 10.7 Onde consultar

- HTML: `target/site/jacoco/index.html`;
- XML: `target/site/jacoco/jacoco.xml`;
- CSV: `target/site/jacoco/jacoco.csv`;
- dados de execução: `target/jacoco.exec`.

## 11. Falha controlada do portão

Para demonstrar que o portão realmente bloqueia o build, o limite de branches foi alterado temporariamente de `0.40` para `0.90`.

Configuração temporária:

```xml
<jacoco.minimum.branch.coverage>0.90</jacoco.minimum.branch.coverage>
```

Resultado do `.\mvnw.cmd verify`:

```text
[WARNING] Rule violated for bundle alsports: branches covered ratio is 0.71, but expected minimum is 0.90
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.16:check
        (report-and-check-coverage) on project alsports:
        Coverage checks have not been met.
```

A falha ocorreu no `jacoco:check`, com os 353 testes aprovados. Não foi erro de compilação, de infraestrutura ou de teste.

O valor foi restaurado imediatamente para `0.40`; a busca por `0.90` no `pom.xml` não retornou ocorrências. O `clean verify` final voltou a passar com `All coverage checks have been met.`

## 12. Estratégia adotada nesta aula

### 12.1 O que foi aprimorado

`ContaAcessoTest` já existia com 16 testes em AssertJ. Em vez de reescrever a classe inteira:

- o teste de criação passou a usar `assertAll`, verificando todos os atributos iniciais;
- o teste de login em branco virou parametrizado com `@NullAndEmptySource` e `@ValueSource`, cobrindo nulo, vazio, espaço e múltiplos espaços;
- foi criado `deveAlterarSenhaSomenteQuandoOHashForValido`, com `assertThrows` para hash em branco e nulo, garantindo que o hash anterior é preservado.

Resultado: 20 execuções em `ContaAcessoTest`, todas aprovadas, executadas isoladamente em 0,348 s.

### 12.2 O que foi criado

`ClienteServiceTest` cobre:

- cadastro para pessoa ativa, com normalização de telefone;
- pessoa inexistente;
- pessoa inativa;
- cliente duplicado para a mesma pessoa;
- busca por id e cliente inexistente;
- ordenação inválida;
- paginação ausente;
- pesquisa com ordenação permitida;
- atualização de telefone;
- atualização de cliente inexistente.

São 11 testes, todos unitários de aplicação, com Mockito e sem Spring.

### 12.3 O que não foi feito

- não foram criados testes para `ErroResponse`, pois a classe não é usada no código de produção;
- não foi criado teste artificial para `AdministradorInicialRunner`, pois a execução depende de configuração externa;
- não foram removidos testes existentes;
- não foi alterada nenhuma regra de negócio para aumentar cobertura;
- não foram criadas exclusões de pacotes ou classes no JaCoCo.

## 13. Qualidade da suíte existente

### 13.1 Asserções

A maior parte das asserções verifica resultados observáveis: status, corpo, exceção, atributos da entidade. Há uso pontual de `verify` para confirmar que uma operação proibida não chegou ao repositório, o que é parte do contrato.

### 13.2 Independência e isolamento

- testes de domínio usam objetos novos em `@BeforeEach`;
- testes de aplicação usam mocks novos por extensão do Mockito;
- testes de integração criam e removem seus próprios dados;
- testes de concorrência usam threads e `CountDownLatch`, com timeout nos `Future`.

### 13.3 Riscos de flakiness

- testes de concorrência dependem do tempo e do banco real;
- testes de integração dependem do PostgreSQL disponível;
- o uso de MockMvc com filtros ativos depende da chave JWT de teste.

A suíte não apresentou falhas intermitentes nesta execução, mas esses riscos existem e devem ser monitorados.

## 14. Comandos

Executar a suíte completa:

```powershell
.\mvnw.cmd clean test
```

Executar uma classe isolada:

```powershell
.\mvnw.cmd "-Dtest=ContaAcessoTest" test
.\mvnw.cmd "-Dtest=ClienteServiceTest" test
```

Executar o ciclo completo com cobertura e portão:

```powershell
.\mvnw.cmd clean verify
```

Abrir o relatório:

```text
target/site/jacoco/index.html
```

O profile de teste exige `DB_TEST_PASSWORD` disponível no processo. Não registre o valor em arquivos versionados nem na documentação.

## 15. Diagnóstico orientado por evidências

### 15.1 O build passa, mas a cobertura caiu

Verifique se novos métodos, branches ou classes foram adicionados sem teste. Compare `jacoco.csv` antes e depois.

### 15.2 O portão falha mesmo com testes verdes

A falha do `jacoco:check` é independente dos testes. Leia a mensagem `Rule violated for bundle ...` e confira o contador e o limite.

### 15.3 A cobertura de uma classe é 0%

Pode ser código morto, execução condicional ou fluxo não exercitado. Não crie teste artificial apenas para mover o percentual; avalie o risco.

### 15.4 O teste unitário passa, mas a integração falha

O mock pode estar escondendo uma constraint, um `flush()` ausente ou uma diferença de schema. Confira o teste de integração correspondente.

### 15.5 O teste de concorrência falha de forma intermitente

Verifique timeout, ordem dos locks, isolamento dos dados e disponibilidade do PostgreSQL. Não aumente o timeout sem investigar a causa.

### 15.6 O agente JaCoCo não foi carregado

Confira no log a linha `argLine set to -javaagent:...jacoco.agent...`. Se ela não aparecer, o `prepare-agent` não executou ou o `argLine` foi sobrescrito.

## 16. Segurança e qualidade

### 16.1 Não usar banco de desenvolvimento ou produção

O profile `test` deve apontar exclusivamente para o banco de testes. Um teste destrutivo em produção é um incidente, não um teste.

### 16.2 Não registrar segredos

Senhas, tokens, chaves e credenciais não pertencem a testes, documentação, logs ou relatórios. A credencial de teste deve ser fornecida pelo ambiente.

### 16.3 Não desativar a segurança para testar

Os testes de segurança mantêm os filtros ativos. Desativar a cadeia de filtros nesses cenários anularia a evidência.

### 16.4 Não esconder falhas

Testes ignorados, exclusões de cobertura e asserções enfraquecidas reduzem a evidência. O relatório deve registrar limitações, não mascará-las.

### 16.5 Cobertura não é autorização para relaxar

Um percentual alto não justifica remover validações, reduzir asserções ou excluir classes. A cobertura é um indicador, não um objetivo isolado.

## 17. Limitações e decisões

### 17.1 Sem testes de mutação

A suíte mede execução, não a qualidade das asserções. Um teste fraco pode cobrir muita linha sem detectar regressões.

### 17.2 Sem testes de contrato externo

A API não possui consumidores externos formalizados. O contrato OpenAPI é verificado internamente.

### 17.3 Sem testes de carga

Não há medição de throughput, latência ou saturação. Os testes de concorrência validam correção, não desempenho.

### 17.4 Cobertura de DTOs e classes de infraestrutura

DTOs de entrada sem fluxo exercitado e classes condicionais permanecem com cobertura baixa. Isso é registrado como lacuna, não corrigido com testes artificiais.

### 17.5 Baseline medida após configurar o JaCoCo

A baseline foi medida com o plugin já adicionado ao `pom.xml`, mas antes dos novos testes. A ordem está documentada para que os números sejam interpretáveis.

### 17.6 Contagem de testes diferente da referência do professor

O material de referência apresenta oito execuções para `ContaAcessoTest`. O AL Sports evoluiu e possui 20 execuções nessa classe, sem testes artificiais. As contagens não devem ser comparadas diretamente.

### 17.7 TDD não simulado

Não foi criada uma falha artificial no código de produção para simular Red–Green–Refactor. A evidência real de Red é a falha controlada do portão de cobertura.

## 18. Checklist

- [x] material oficial lido integralmente;
- [x] worktree isolada criada a partir da Aula 14;
- [x] backups duplos com manifesto SHA-256 conferido;
- [x] baseline executada antes dos novos testes;
- [x] cobertura inicial medida;
- [x] `ContaAcessoTest` aprimorado com `assertAll`, `assertThrows` e parametrização;
- [x] `ClienteServiceTest` criado para lacuna real;
- [x] JaCoCo 0.8.16 configurado com `prepare-agent`, `report` e `check`;
- [x] regra `BUNDLE` com `LINE` e `BRANCH` em `COVEREDRATIO`;
- [x] limites de 70% de linhas e 40% de branches mantidos;
- [x] `clean verify` aprovado;
- [x] relatório HTML gerado;
- [x] falha controlada do portão demonstrada;
- [x] limite definitivo restaurado;
- [x] `clean verify` final aprovado;
- [x] documentação criada;
- [x] README atualizado;
- [x] nenhum segredo adicionado;
- [x] `.env` não alterado;
- [x] nenhum arquivo staged;
- [x] nenhum commit, tag ou push.

## 19. Commit e tag sugeridos

Somente como sugestão, sem execução nesta etapa:

```text
feat: aprofunda testes e adiciona cobertura com JaCoCo
```

Tag anotada:

```text
aula-15-testes-cobertura
```

Mensagem da tag:

```text
Aula 15 - testes de software e cobertura
```

## Referências

- material oficial da Aula 15 do curso;
- documentação do JUnit Jupiter;
- documentação do Mockito;
- documentação do JaCoCo 0.8.16;
- documentação do Spring Boot Test e MockMvc;
- documentação do PostgreSQL e do Liquibase;
- documentação das Aulas 10 a 14 do AL Sports.
