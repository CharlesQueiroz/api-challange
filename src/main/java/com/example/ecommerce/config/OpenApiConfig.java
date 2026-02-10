package com.example.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.contact.email:charles.queiroz@dazen.com.br}")
    private String contactEmail;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API")
                        .description("REST API for managing products, orders, and order items")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Charles Queiroz")
                                .email(contactEmail)));
    }
}
