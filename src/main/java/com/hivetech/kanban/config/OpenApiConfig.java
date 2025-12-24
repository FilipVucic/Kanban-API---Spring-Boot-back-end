package com.hivetech.kanban.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Kanban API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for managing Kanban board tasks.
                                
                                ## Features
                                - Full CRUD operations for tasks
                                - Pagination and filtering support
                                - WebSocket real-time notifications
                                - JWT authentication
                                - Optimistic locking for concurrent updates
                                
                                ## Authentication
                                Use the `/auth/register` endpoint to register a new user and obtain a JWT token.
                                Then include the token in the Authorization header as: `Bearer <token>`
                                """)
                        .contact(new Contact()
                                .name("Filip Vucic")
                                .email("filip.vucic3@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token in the format: `<token>`")));
    }
}

