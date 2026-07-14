package co.edu.escuelaing.techcup.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techcup.security.internal")
public record InternalApiKeyProperties(String apiKey) {
}
