package co.edu.escuelaing.techcup.match.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Punto único de verificación del rol "servicio" (autenticado por {@link InternalApiKeyFilter}),
 * usado desde @PreAuthorize en los endpoints servicio-a-servicio (p. ej. recepción de la
 * definición del partido enviada por Tournament). A diferencia de {@link RefereeGuard}, este
 * rol nunca proviene de un JWT de usuario.
 */
@Component("serviceGuard")
public class ServiceGuard {

    private static final String ROLE_SERVICE = "ROLE_SERVICE";

    public boolean isService() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(ROLE_SERVICE));
    }
}
