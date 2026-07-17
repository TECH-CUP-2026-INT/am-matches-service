package co.edu.escuelaing.techcup.match.security;

import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica llamadas servicio-a-servicio (p. ej. Tournament recibiendo/enviando la
 * definición del partido) mediante una clave compartida, en vez de un JWT de usuario.
 * Mismo esquema que ya expone el servicio de notificaciones (ver comentario en
 * RestSanctionNotifier): header {@value #HEADER_NAME} comparado contra
 * {@code techcup.security.internal.api-key}.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final InternalApiKeyProperties properties;

    public InternalApiKeyFilter(InternalApiKeyProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank() && header.equals(properties.apiKey())) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
