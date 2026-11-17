package com.curso.alsports.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alsportsOpenAPI() {

        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                    "Autenticação Bearer com JWT emitido por "
                        + "POST /api/autenticacao/login. O token tem validade "
                        + "absoluta de 30 minutos. Em produção, use somente "
                        + "sobre HTTPS/TLS.");

        return new OpenAPI()
            .info(new Info()
                .title("AL Sports API")
                .description(
                    "API REST para gerenciamento de produtos, "
                        + "categorias, fornecedores, estoque, vendas e acessos da AL Sports. "
                        + "As operações são autenticadas por Bearer Token (JWT), exceto as "
                        + "rotas públicas de saúde, documentação, cadastro inicial e login.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("AL Sports")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", bearerAuth))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
