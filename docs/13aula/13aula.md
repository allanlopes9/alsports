# Aula 13 — JWT e Bearer Token

## Apresentação

Na Aula 12, a API passou a exigir autenticação com HTTP Basic. A cada requisição protegida, o cliente enviava login e senha no cabeçalho `Authorization`. Nesta aula, esse modelo é substituído por Bearer Token com JWT assinado.

O fluxo passa a ter duas etapas:

1. o cliente troca login e senha por um access token no endpoint público `POST /api/autenticacao/login`;
2. o cliente envia somente o token nas requisições seguintes, no cabeçalho `Authorization: Bearer <token>`.

A autenticação continua stateless. O servidor não cria sessão, não mantém cookie e não guarda o token em banco. A diferença é que a credencial de longo prazo (login e senha) deixa de circular em cada requisição.

## Resultados de aprendizagem

Ao final desta aula, você deve ser capaz de:

- explicar a diferença entre HTTP Basic e Bearer Token;
- descrever a estrutura de um JWT;
- diferenciar assinatura de criptografia;
- configurar um OAuth2 Resource Server no Spring Security;
- emitir tokens HS256 com emissor, expiração e claims mínimas;
- validar assinatura, emissor e expiração sem implementar criptografia manualmente;
- proteger rotas com Bearer Token e manter rotas públicas explícitas;
- documentar o esquema `bearerAuth` no OpenAPI;
- testar emissão, uso e rejeição de tokens;
- reconhecer as limitações de revogação de um JWT stateless.

## Pré-requisitos

- projeto da Aula 12 compilando e com os testes aprovados;
- PostgreSQL disponível para o profile de teste;
- Java 21;
- Maven Wrapper;
- noções de HTTP, JSON e Spring Security.

## 1. Do HTTP Basic para o Bearer Token

HTTP Basic envia login e senha codificados em Base64 em cada requisição. Base64 não é criptografia: qualquer interceptação sem TLS expõe a senha. Além disso, a senha precisa ser conferida novamente em todas as chamadas.

No modelo Bearer:

- a senha é usada apenas no login;
- o servidor devolve um token assinado;
- o cliente apresenta o token nas requisições seguintes;
- o servidor valida a assinatura e as claims sem consultar a senha.

Bearer significa “portador”. Quem possui o token pode usá-lo até a expiração. Por isso o token deve ser tratado como segredo e transportado somente sobre HTTPS/TLS.

### 1.1 Stateless não significa sem estado em todo o sistema

A API continua sem sessão HTTP. O estado necessário para validar o token está dentro do próprio JWT e na chave configurada no servidor. O banco continua armazenando pessoas, contas e hashes, mas não precisa armazenar a sessão nem o token.

Stateless também não significa que o token nunca possa ser invalidado. Significa que a validação padrão não consulta um estado central por requisição. Nesta aula, não há blacklist nem consulta de revogação.

## 2. Anatomia de um JWT

Um JWT é composto por três partes separadas por ponto:

```text
header.payload.signature
```

Cada parte é codificada em Base64URL.

### 2.1 Header

O header identifica o tipo e o algoritmo:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Nesta aula, o algoritmo é fixado em HS256 no encoder e no decoder. O servidor não aceita que o token escolha livremente outro algoritmo.

### 2.2 Payload

O payload contém as claims. Exemplo didático:

```json
{
  "iss": "https://alsports.local",
  "sub": "ana.souza",
  "iat": 1763402400,
  "exp": 1763404200,
  "jti": "8f14e45f-ea3f-4b1c-9c2f-3b7a1d5e6f70",
  "perfis": ["OPERADOR"]
}
```

Significado:

- `iss` — emissor do token;
- `sub` — sujeito, neste projeto o login canônico da conta;
- `iat` — instante de emissão;
- `exp` — instante absoluto de expiração;
- `jti` — identificador único do token;
- `perfis` — perfis derivados das authorities autenticadas.

O payload não contém senha, hash, CPF nem segredo. O `sub` é suficiente para localizar a conta quando uma rota precisa de dados atuais, como `/me`.

### 2.3 Assinatura

A assinatura protege a integridade do header e do payload. Com HS256, o servidor usa uma chave secreta compartilhada para assinar e validar. Se alguém alterar uma claim, a assinatura deixa de conferir e o token é rejeitado.

A assinatura não esconde o conteúdo. Ela apenas permite detectar alterações e comprovar que o token foi emitido por quem possui a chave.

### 2.4 JWT assinado não é criptografado

Qualquer pessoa que possua o token consegue ler o payload. Base64URL é codificação, não criptografia.

Consequências práticas:

- não incluir senha, hash, CPF, cartão ou dados sensíveis desnecessários;
- não considerar o payload secreto;
- transportar o token somente sobre TLS;
- assumir que o token pode ser reutilizado se for interceptado.

## 3. HS256, RS256 e a decisão didática

HS256 usa uma chave simétrica: a mesma chave assina e valida. É simples, rápido e adequado quando emissor e validador são a mesma aplicação ou confiam na mesma chave.

RS256 usa par de chaves: a chave privada assina e a pública valida. É comum em Authorization Servers que publicam um JWK Set para vários Resource Servers.

Nesta aula, a aplicação é um Resource Server e também um emissor didático para o próprio login. Por isso foi escolhido HS256, com uma única chave configurada em `JWT_SECRET`. Não há Authorization Server completo, JWK Set, RSA ou rotação de chaves.

## 4. Dependência

A dependência oficial do Resource Server é:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

Ela fornece o filtro Bearer, o suporte JOSE e os componentes de validação. Não é necessário criar filtro JWT artesanal, fazer parsing manual do token ou adicionar bibliotecas redundantes.

Valide com:

```powershell
.\mvnw.cmd -DskipTests compile
```

## 5. Gerando a chave no `.env`

A chave nunca deve estar em Java, `application.properties`, documentação, imagem Docker ou Git.

### Windows PowerShell

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RNGCryptoServiceProvider]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

### macOS e Linux

```bash
openssl rand -base64 32
```

Copie o resultado para o `.env` local:

```dotenv
JWT_SECRET=resultado-base64-gerado-na-sua-maquina
```

Cada aluno e cada ambiente devem gerar um valor diferente. O `.env` é ignorado pelo Git e não deve ser compartilhado.

O `.env.example` contém apenas um placeholder:

```dotenv
JWT_SECRET=substitua-por-uma-chave-base64-de-256-bits
```

Esse valor é fictício e não funciona como chave.

O profile de teste possui uma chave didática própria, versionada somente em `src/test/resources/application-test.properties`. Ela não deve ser usada em desenvolvimento compartilhado nem em produção.

## 6. Configuração tipada

As propriedades ficam em `application.properties`:

```properties
app.security.jwt.issuer=https://alsports.local
app.security.jwt.expiration=PT30M
app.security.jwt.secret=${JWT_SECRET}
```

### 6.1 JwtProperties

```java
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        Duration expiration,
        String secret) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.issuer é obrigatório");
        }

        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException(
                    "app.security.jwt.expiration deve ser uma duração positiva");
        }

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET é obrigatória para assinar e validar tokens");
        }
    }
}
```

A validação estrutural ocorre na inicialização. Se a chave estiver ausente, a aplicação não sobe.

### 6.2 JwtConfig

```java
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final int TAMANHO_MINIMO_CHAVE_BYTES = 32;
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] chave;

        try {
            chave = Base64.getDecoder().decode(properties.secret().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET deve estar codificada em Base64", ex);
        }

        if (chave.length < TAMANHO_MINIMO_CHAVE_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve possuir pelo menos 32 bytes após a "
                            + "decodificação Base64");
        }

        return new SecretKeySpec(chave, ALGORITMO_HMAC);
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            JwtProperties properties) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuer()));

        return decoder;
    }
}
```

O `Clock` é injetado para permitir testes determinísticos. O decoder valida assinatura, emissor e claims temporais padrão.

## 7. Reutilizando a autenticação da Aula 12

O login não cria um segundo fluxo de autenticação. O `SecurityConfig` expõe um `ProviderManager` explícito com o mesmo `DaoAuthenticationProvider`, `ContaAcessoUserDetailsService` e `PasswordEncoder` da Aula 12.

```java
@Bean
public AuthenticationProvider authenticationProvider(
        ContaAcessoUserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {

    DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
}

@Bean
public AuthenticationManager authenticationManager(
        AuthenticationProvider authenticationProvider) {

    return new ProviderManager(authenticationProvider);
}
```

Vantagens:

- a comparação BCrypt continua em um único lugar;
- conta bloqueada e pessoa inativa continuam sendo verificadas pelo provider;
- não há consulta paralela nem segunda validação de senha;
- o comportamento da Aula 12 permanece coberto pelos testes.

### 7.1 AutenticacaoService

```java
@Service
public class AutenticacaoService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS =
            "Login ou senha inválidos";

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AutenticacaoService(
            AuthenticationManager authenticationManager,
            TokenService tokenService) {

        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public TokenAcesso autenticar(String login, String senha) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            login, senha));

            return tokenService.emitir(authentication);
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }
    }
}
```

Qualquer falha de autenticação vira a mesma exceção pública. O cliente não descobre se o login existe, se a senha está errada ou se a conta está bloqueada.

## 8. Emitindo o token

O `TokenService` monta as claims a partir do objeto `Authentication` já validado.

```java
Instant emitidoEm = clock.instant();
Instant expiraEm = emitidoEm.plus(properties.expiration());

List<String> perfis = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(autoridade -> autoridade != null
                && autoridade.startsWith("ROLE_"))
        .map(autoridade -> autoridade.substring("ROLE_".length()))
        .filter(perfil -> !perfil.isBlank())
        .distinct()
        .sorted()
        .toList();

JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .subject(authentication.getName())
        .issuedAt(emitidoEm)
        .expiresAt(expiraEm)
        .id(UUID.randomUUID().toString())
        .claim("perfis", perfis)
        .build();
```

A validade é absoluta:

```text
exp = iat + app.security.jwt.expiration
```

Com `PT30M`, o resultado é 1800 segundos. O uso do token não altera `iat` nem `exp`.

## 9. Endpoint de login

O endpoint é público:

```http
POST /api/autenticacao/login
Content-Type: application/json
```

Request:

```json
{
  "login": "ana.souza",
  "senha": "uma-frase-senha-2026"
}
```

Resposta `200 OK`:

```json
{
  "accessToken": "<jwt-assinado>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "expiresAt": "2026-11-17T21:30:00Z"
}
```

Campos:

- `accessToken` — JWT assinado;
- `tokenType` — sempre `Bearer`;
- `expiresIn` — validade em segundos;
- `expiresAt` — instante absoluto de expiração.

Erros:

- `400 Bad Request` — corpo inválido, login ou senha em branco ou acima do limite;
- `401 Unauthorized` — credenciais inválidas, conta bloqueada ou pessoa inativa.

O `401` do login usa o formato `ApiError`:

```json
{
  "timestamp": "2026-11-17T21:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Login ou senha inválidos",
  "path": "/api/autenticacao/login",
  "fields": null
}
```

A mensagem é intencionalmente genérica.

## 10. Resource Server

O `SecurityConfig` mantém a API stateless, remove HTTP Basic e habilita o Resource Server JWT:

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers(
            "/api/health",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**")
        .permitAll()
        .requestMatchers(
            HttpMethod.POST,
            "/api/pessoas",
            "/api/contas-acesso",
            "/api/autenticacao/login")
        .permitAll()
        .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(Customizer.withDefaults()))
    .authenticationProvider(authenticationProvider);
```

Rotas públicas:

- `GET /api/health`;
- `GET /v3/api-docs/**`;
- `/swagger-ui.html` e `/swagger-ui/**`;
- `POST /api/pessoas`;
- `POST /api/contas-acesso`;
- `POST /api/autenticacao/login`.

Todas as demais exigem Bearer Token válido.

O CSRF continua desabilitado somente porque não há sessão nem cookie de autenticação. Se cookies forem introduzidos no futuro, essa decisão deve ser reavaliada.

## 11. Comportamento 401

O Resource Server mantém o comportamento padrão do Spring Security. Não foi criado `AuthenticationEntryPoint` customizado para Bearer.

São rejeitados com `401 Unauthorized`:

- ausência de token;
- token malformado;
- token adulterado;
- token expirado;
- token com emissor incorreto;
- token assinado com outra chave;
- token com algoritmo não permitido, como `alg: none`;
- tentativa de autenticação com HTTP Basic.

A resposta inclui `WWW-Authenticate: Bearer` e não expõe stack trace, SQL ou detalhes internos.

O login é a exceção: como a falha ocorre na camada de aplicação, ele responde `401` com `ApiError` genérico.

## 12. OpenAPI e Swagger UI

O esquema documentado passou a ser:

```java
SecurityScheme bearerAuth = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description(
            "Autenticação Bearer com JWT emitido por "
                + "POST /api/autenticacao/login. O token tem validade "
                + "absoluta de 30 minutos. Em produção, use somente "
                + "sobre HTTPS/TLS.");
```

O requisito de segurança é global, e o endpoint de login é anotado com `@SecurityRequirements` para ficar público na documentação. Não há resquício de `basicAuth`.

Fluxo no Swagger UI:

1. execute `POST /api/autenticacao/login`;
2. copie somente `accessToken`;
3. clique em **Authorize**;
4. cole o token;
5. execute `/api/contas-acesso/me`.

Em versões atuais do Swagger UI, o prefixo `Bearer` é acrescentado pelo esquema. Se a interface solicitar o cabeçalho completo, siga a indicação exibida.

## 13. Postman

A collection mantém o nome do arquivo:

```text
docs/postman/AL-Sports-Aula-07.postman_collection.json
```

O nome interno foi atualizado para `AL Sports - Aula 13`.

A autenticação raiz agora é:

```json
{
  "type": "bearer",
  "bearer": [
    { "key": "token", "value": "{{accessToken}}", "type": "string" }
  ]
}
```

A requisição de login usa `No Auth` e salva o token:

```javascript
const body = pm.response.json();
if (pm.response.code === 200) {
    pm.collectionVariables.set('accessToken', body.accessToken);
}
```

Fluxo da pasta `07 - Aula 13`:

1. login e salvamento do token;
2. consulta de `/me` herdando Bearer;
3. rota protegida de produtos com Bearer;
4. login com senha incorreta retornando 401;
5. token inválido retornando 401;
6. HTTP Basic retornando 401.

As pastas seguintes reutilizam a variável até o token expirar. Se receber `401` após 30 minutos, execute novamente apenas o login.

A collection não contém token fixo, senha real nem chave JWT.

## 14. Testes automatizados

A suíte da Aula 13 possui 283 testes em 24 classes, todos aprovados.

Principais classes:

- `AutenticacaoSecurityTest` — integração do login e do Resource Server com Bearer real;
- `ContaAcessoSecurityTest` — regressão dos fluxos de conta migrados para Bearer;
- `JwtConfigTest` — chave, configuração e validação de tokens;
- `TokenServiceTest` — emissão, claims, `jti` e expiração absoluta;
- `AutenticacaoServiceTest` — reutilização do `AuthenticationManager` e falha sem emissão;
- `OpenApiDocumentationTest` — contrato `bearerAuth` e login público.

Cenários cobertos:

- contrato do login e claims obrigatórias;
- `exp = iat + 1800`;
- `jti` único;
- `sub` canônico sem diferenciar caixa;
- `perfis` derivados das authorities;
- ausência de senha, hash, CPF e segredo no payload;
- token válido em `/me` e em rotas de produtos;
- ausência de token, token malformado, adulterado, expirado, com emissor errado, com outra chave ou `alg: none`;
- HTTP Basic rejeitado;
- credenciais inválidas, conta bloqueada e pessoa inativa com 401 genérico;
- validação estrutural com 400;
- token ainda válido após bloqueio e após troca de senha;
- OpenAPI sem `basicAuth` e com `bearerAuth`.

Os testes que precisam controlar o tempo usam `Clock` fixo no `TokenService` e tokens com `exp` no passado para o decoder. A tolerância padrão de relógio do validador é considerada.

Para executar:

```powershell
.\mvnw.cmd test
```

## 15. Limitações e decisões

### 15.1 Expiração absoluta

O token expira em 30 minutos contados da emissão. Não há renovação por uso, sliding expiration ou idle timeout. Depois de `exp`, é necessário autenticar novamente.

### 15.2 Sem refresh token

Não existe refresh token nesta aula. O cliente não troca um token expirado por outro sem informar as credenciais novamente.

### 15.3 Sem logout no servidor

O logout é responsabilidade do cliente: basta descartar o token. O servidor não mantém lista de tokens ativos.

### 15.4 Sem revogação nem blacklist

Não há blacklist, tabela de tokens, revogação por `jti` ou consulta de estado por requisição. Um token já emitido continua válido até `exp`.

### 15.5 Bloqueio e troca de senha

Bloquear a conta ou trocar a senha impede novos logins, mas não invalida tokens já emitidos. Essa limitação é intencional e está coberta por testes. Uma solução completa exigiria revogação, versão de credencial ou consulta de estado, temas de etapas posteriores.

### 15.6 Emissor didático

A aplicação emite e valida os próprios tokens para fins didáticos. Ela não é um Authorization Server OAuth2 completo, não implementa fluxo de autorização, não publica JWK Set e não atende outros clientes.

### 15.7 Sem autorização por perfil

O claim `perfis` é emitido, mas esta aula não implementa `hasRole`, `@PreAuthorize` nem regras de autorização por perfil. Todos os usuários autenticados acessam as mesmas rotas protegidas. A autorização por perfil é o tema da Aula 14.

### 15.8 JWT não é criptografia

O token é assinado, não cifrado. O payload é legível. Não inclua dados sensíveis.

## 16. Checklist

- [x] Dependência oficial do Resource Server adicionada.
- [x] Chave gerada localmente e mantida fora do Git.
- [x] `.env.example` apenas com placeholder.
- [x] Configuração tipada com issuer, expiração e segredo.
- [x] Validação de Base64 e mínimo de 32 bytes.
- [x] HS256 fixado no encoder e no decoder.
- [x] `Clock` injetado.
- [x] `ProviderManager` reutilizando o provider da Aula 12.
- [x] Login público com resposta `accessToken`, `tokenType`, `expiresIn` e `expiresAt`.
- [x] Claims `iss`, `sub`, `iat`, `exp`, `jti` e `perfis`.
- [x] Expiração absoluta de 30 minutos.
- [x] Security stateless e sem HTTP Basic.
- [x] Rotas públicas explícitas.
- [x] Bearer inválido retornando 401.
- [x] Login inválido retornando 401 com `ApiError` genérico.
- [x] OpenAPI com `bearerAuth`.
- [x] Postman com Bearer e salvamento automático do token.
- [x] Testes de emissão, uso, validação e regressão.
- [x] Documentação das limitações de revogação.

## 17. Commit e tag sugeridos

Commit:

```text
feat: implementa jwt e bearer token na aula 13
```

Tag:

```text
aula-13-jwt-bearer-token
```

Mensagem da tag:

```text
Aula 13 - JWT e Bearer Token
```

## Referências

- Documentação oficial do Spring Security — OAuth2 Resource Server JWT.
- Documentação oficial do Spring Boot — propriedades de configuração.
- RFC 7519 — JSON Web Token.
- RFC 7515 — JSON Web Signature.
- RFC 6750 — The OAuth 2.0 Authorization Framework: Bearer Token Usage.
- Material da Aula 13 do curso.
