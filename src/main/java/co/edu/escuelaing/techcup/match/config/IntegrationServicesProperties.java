package co.edu.escuelaing.techcup.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techcup.servicios")
public record IntegrationServicesProperties(
        ServiceEndpoint estadisticas,
        ServiceEndpoint notificaciones
) {

    public record ServiceEndpoint(String baseUrl) {
    }
}
