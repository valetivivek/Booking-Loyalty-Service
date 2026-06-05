package com.vivek.bookingloyalty.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata plus a "bearerAuth" scheme, so the Swagger UI shows an
 * Authorize button where you can paste a JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI bookingLoyaltyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking & Loyalty Service API")
                        .description("Resort/hotel booking backend with JWT auth, RBAC, and a loyalty program")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
