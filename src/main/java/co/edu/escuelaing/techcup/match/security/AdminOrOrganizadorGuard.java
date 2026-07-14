package co.edu.escuelaing.techcup.match.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Punto único de verificación de los roles "admin"/"organizador", usado desde @PreAuthorize
 * en los controllers que exponen el audit log local de eventos de partido. A diferencia del
 * rol árbitro (ver {@link RefereeGuard}), estos roles no tienen una propiedad de configuración
 * dedicada porque el nombre del claim es fijo y no varía entre entornos.
 */
@Component("adminOrOrganizadorGuard")
public class AdminOrOrganizadorGuard {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_ORGANIZADOR = "ROLE_ORGANIZADOR";

    public boolean isAdminOrOrganizador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(ROLE_ADMIN)
                        || authority.getAuthority().equals(ROLE_ORGANIZADOR));
    }
}
