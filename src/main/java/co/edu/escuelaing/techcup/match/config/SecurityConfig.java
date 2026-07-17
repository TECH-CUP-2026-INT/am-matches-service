package co.edu.escuelaing.techcup.match.config;

import co.edu.escuelaing.techcup.match.security.InternalApiKeyFilter;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtClaimsFilter jwtClaimsFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    public SecurityConfig(JwtClaimsFilter jwtClaimsFilter, InternalApiKeyFilter internalApiKeyFilter) {
        this.jwtClaimsFilter = jwtClaimsFilter;
        this.internalApiKeyFilter = internalApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF explota que el navegador adjunta cookies de sesión automáticamente; esta
                // API es stateless (ver STATELESS abajo) y se autentica con un Bearer JWT o una
                // api key interna que el navegador nunca envía por su cuenta, así que no hay
                // sesión de la que abusar.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 -- API stateless con Bearer JWT, sin cookies no aplica CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtClaimsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
