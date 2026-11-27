package com.smartgarden.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "IntelliGarden API",
                version = "v1",
                description = "API do IntelliGarden: leituras do sensor, água estimada das plantas, pesagens e irrigação simulada.",
                contact = @Contact(name = "Projeto IntelliGarden")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor local")
        }
)
public class OpenApiConfig {
}

