package dev.spironello.fichasrpg.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Esquema Bearer do Swagger: o botão "Authorize" aceita o accessToken e passa a mandá-lo em todas as chamadas.
 * As rotas de /auth tiram o requisito com @SecurityRequirements (são abertas).
 */
@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class DocumentacaoConfig {
}
