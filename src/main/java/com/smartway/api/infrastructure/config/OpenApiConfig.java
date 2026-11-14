package com.smartway.api.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI smartWayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SmartWay API")
                .description("Assistente Virtual Inteligente para Mobilidade Urbana")
                .version("2.0.0")
                .contact(new Contact().name("SmartWay Team").email("contato@smartway.com.br"))
                .license(new License().name("MIT")));
    }
}
