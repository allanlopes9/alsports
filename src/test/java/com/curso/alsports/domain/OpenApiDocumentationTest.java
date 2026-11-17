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
            .andExpect(jsonPath("$.tags[?(@.name == 'Vendas')]").exists());
    }
}
