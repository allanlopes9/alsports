package com.curso.alsports.domain;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePublicarContratoOpenApiComRotasEssenciais() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.")))
            .andExpect(jsonPath("$.info.title").value("AL Sports API"))
            .andExpect(jsonPath("$.info.version").value("1.0.0"))

            // Categorias
            .andExpect(jsonPath("$.paths['/api/categorias/{id}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/categorias/{id}'].put").exists())

            // Produtos
            .andExpect(jsonPath("$.paths['/api/produtos/{id}'].get.summary")
                .value("Buscar produto por ID"))
            .andExpect(jsonPath("$.paths['/api/produtos/{id}'].get.responses['404'].content").exists())
            .andExpect(jsonPath("$.paths['/api/produtos/{id}'].delete").exists())

            // Movimentações de estoque
            .andExpect(jsonPath("$.paths['/api/produtos/{id}/estoque/entradas'].post").exists())
            .andExpect(jsonPath("$.paths['/api/produtos/{id}/estoque/saidas'].post").exists())

            // Pessoas
            .andExpect(jsonPath("$.paths['/api/pessoas'].post").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas'].get").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas/{id}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas/{id}'].put").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas/{id}/status'].put").exists())

            // Clientes e colaboradores
            .andExpect(jsonPath("$.paths['/api/clientes'].post").exists())
            .andExpect(jsonPath("$.paths['/api/clientes/{id}'].put").exists())
            .andExpect(jsonPath("$.paths['/api/colaboradores'].post").exists())
            .andExpect(jsonPath("$.paths['/api/colaboradores/{id}'].put").exists())

            // Vendas
            .andExpect(jsonPath("$.paths['/api/vendas'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vendas'].get").exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{id}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{id}/itens'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{vendaId}/itens/{itemId}'].delete").exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{id}/finalizacao'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{id}/finalizacao'].post.description")
                .value(containsString("ordem determinística")))
            .andExpect(jsonPath("$.paths['/api/produtos/{id}/estoque/entradas'].post.description")
                .value(containsString("Bloqueia o produto")))
            .andExpect(jsonPath("$.paths['/api/produtos/{id}/estoque/saidas'].post.description")
                .value(containsString("Bloqueia o produto")))
            .andExpect(jsonPath("$.paths['/api/vendas/{id}/cancelamento'].post").exists())

            // Schemas
            .andExpect(jsonPath("$.components.schemas.ProdutoResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists())
            .andExpect(jsonPath("$.components.schemas.PessoaRequest").exists())
            .andExpect(jsonPath("$.components.schemas.PessoaResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ClienteRequest").exists())
            .andExpect(jsonPath("$.components.schemas.ClienteResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ColaboradorRequest").exists())
            .andExpect(jsonPath("$.components.schemas.ColaboradorResponse").exists())
            .andExpect(jsonPath("$.components.schemas.VendaRequest").exists())
            .andExpect(jsonPath("$.components.schemas.VendaResponse").exists())
            .andExpect(jsonPath("$.components.schemas.VendaResumoResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ItemVendaRequest").exists())
            .andExpect(jsonPath("$.components.schemas.ItemVendaResponse").exists())

            // Quantidades decimais com até três casas
            .andExpect(jsonPath(
                "$.components.schemas.ItemVendaRequest.properties.quantidade.type")
                .value("number"))
            .andExpect(jsonPath(
                "$.components.schemas.ItemVendaRequest.properties.quantidade.example")
                .value("1.5"))
            .andExpect(jsonPath(
                "$.components.schemas.ItemVendaResponse.properties.quantidade.type")
                .value("number"))
            .andExpect(jsonPath(
                "$.components.schemas.ProdutoResponse.properties.quantidade.type")
                .value("number"))

            // Tags
            .andExpect(jsonPath("$.tags[?(@.name == 'Pessoas')]").exists())
            .andExpect(jsonPath("$.tags[?(@.name == 'Clientes')]").exists())
            .andExpect(jsonPath("$.tags[?(@.name == 'Colaboradores')]").exists())
            .andExpect(jsonPath("$.tags[?(@.name == 'Vendas')]").exists())
            .andExpect(jsonPath("$.tags[?(@.name == 'Contas de acesso')]").exists())

            // Segurança Bearer/JWT documentada globalmente
            .andExpect(jsonPath(
                "$.components.securitySchemes.bearerAuth.type")
                .value("http"))
            .andExpect(jsonPath(
                "$.components.securitySchemes.bearerAuth.scheme")
                .value("bearer"))
            .andExpect(jsonPath(
                "$.components.securitySchemes.bearerAuth.bearerFormat")
                .value("JWT"))
            .andExpect(jsonPath(
                "$.components.securitySchemes.basicAuth")
                .doesNotExist())
            .andExpect(jsonPath("$.security[0].bearerAuth").isArray())

            // Operações públicas sem exigência de credencial
            .andExpect(jsonPath("$.paths['/api/health'].get.security").isEmpty())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/login'].post.security").isEmpty())

            // Operações autenticadas herdam o esquema global; pessoas e
            // criação de conta não são mais públicas.
            .andExpect(jsonPath("$.paths['/api/pessoas'].post.security")
                .doesNotExist())
            .andExpect(jsonPath("$.paths['/api/contas-acesso'].post.security")
                .doesNotExist())

            // Login público
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/login'].post").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/login'].post.responses['200']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/login'].post.responses['400']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/login'].post.responses['401']").exists())
            .andExpect(jsonPath(
                "$.components.schemas.LoginRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.accessToken").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.tokenType").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.expiresIn").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.expiresAt").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.senha")
                .doesNotExist())

            // Contas de acesso
            .andExpect(jsonPath("$.paths['/api/contas-acesso'].post").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso'].post.responses['201']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso'].post.responses['403']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/me'].get").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/me'].get.responses['401']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/me/senha'].put").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/me/senha'].put.responses['204']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/{id}/perfis'].put").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/{id}/perfis'].put.responses['200']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/{id}/perfis'].put.responses['403']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/{id}/status'].put").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/{id}/status'].put.responses['200']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/contas-acesso/{id}/status'].put.responses['403']").exists())
            .andExpect(jsonPath(
                "$.components.schemas.ContaAcessoRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.ContaAcessoResponse").exists())
            .andExpect(jsonPath(
                "$.components.schemas.AlteracaoSenhaRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.PerfisContaAcessoRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.StatusContaAcessoRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.ContaAcessoRequest.properties.senha.format")
                .value("password"))
            .andExpect(jsonPath(
                "$.components.schemas.ContaAcessoResponse.properties.senha")
                .doesNotExist())
            .andExpect(jsonPath(
                "$.components.schemas.ContaAcessoResponse.properties.senhaHash")
                .doesNotExist())

            // Aula 16 - ciclo de tokens
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/refresh'].post").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/refresh'].post.responses['200']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/refresh'].post.responses['401']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/refresh'].post.security").isEmpty())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout'].post").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout'].post.responses['204']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout'].post.security").isEmpty())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout-todos'].post").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout-todos'].post.responses['204']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout-todos'].post.responses['401']").exists())
            .andExpect(jsonPath(
                "$.paths['/api/autenticacao/logout-todos'].post.security")
                .isArray())
            .andExpect(jsonPath(
                "$.components.schemas.RefreshTokenRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.RefreshTokenRequest.properties.refreshToken.format")
                .value("password"))
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.refreshToken").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.refreshTokenExpiresAt").exists())
            .andExpect(jsonPath(
                "$.components.schemas.TokenResponse.properties.tokenHash")
                .doesNotExist())
            .andExpect(jsonPath(
                "$.tags[?(@.name == 'Autenticação')]").exists());
    }
}
