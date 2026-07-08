package co.edu.escuelaing.techcup.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techcup.security")
public record RefereeSecurityProperties(
        String roleClaim,
        String refereeRole
) {
}
