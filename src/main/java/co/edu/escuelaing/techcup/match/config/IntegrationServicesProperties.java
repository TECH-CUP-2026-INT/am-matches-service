package co.edu.escuelaing.techcup.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techcup.servicios")
public record IntegrationServicesProperties(
        ServiceEndpoint competencia,
        ServiceEndpoint estadisticas,
        ServiceEndpoint notificaciones,
        ServiceEndpoint torneos
) {

    public record ServiceEndpoint(String baseUrl) {
    }
}
