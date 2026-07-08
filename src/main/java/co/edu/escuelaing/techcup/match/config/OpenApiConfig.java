package co.edu.escuelaing.techcup.match.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI matchServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Servicio de Partidos - TechCup Fútbol")
                        .description("Arbitraje en tiempo real: inicio, cronómetro, goles, tarjetas, "
                                + "sustituciones, observaciones, planilla y finalización de partido.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .name(BEARER_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT ya validado por el API Gateway. Debe incluir claims "
                                + "\"sub\" (UUID del árbitro) y \"roles\" (debe incluir ARBITRO).")));
    }
}
