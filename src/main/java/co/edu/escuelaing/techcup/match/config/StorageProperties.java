package co.edu.escuelaing.techcup.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "techcup.storage")
public record StorageProperties(
        String matchSheetsDir
) {
}
