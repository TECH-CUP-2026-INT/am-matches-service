package co.edu.escuelaing.techcup.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techcup.sanciones")
public record SanctionProperties(
        int umbralAmarillasPartido
) {
}
