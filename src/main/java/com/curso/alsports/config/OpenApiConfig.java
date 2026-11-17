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

        SecurityScheme basicAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description(
                    "Autenticação HTTP Basic. Em produção, use somente sobre HTTPS/TLS.");

        return new OpenAPI()
            .info(new Info()
                .title("AL Sports API")
                .description(
                    "API REST para gerenciamento de produtos, "
                        + "categorias, fornecedores, estoque, vendas e acessos da AL Sports. "
                        + "As operações são autenticadas por HTTP Basic, exceto as rotas "
                        + "públicas de saúde, documentação e cadastro inicial.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("AL Sports")))
            .components(new Components()
                .addSecuritySchemes("basicAuth", basicAuth))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
