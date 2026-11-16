package com.curso.alsports.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alsportsOpenAPI() {

        return new OpenAPI()
            .info(new Info()
                .title("AL Sports API")
                .description(
                    "API REST para gerenciamento de produtos, "
                        + "categorias, fornecedores e estoque da AL Sports.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("AL Sports")));
    }
}
