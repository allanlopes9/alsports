package com.curso.alsports.domain;

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

            // Schemas
            .andExpect(jsonPath("$.components.schemas.ProdutoResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }
}
