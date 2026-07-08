package co.edu.escuelaing.techcup.match.security;

import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Punto único de verificación del rol "arbitro", usado desde @PreAuthorize en los controllers.
 */
@Component("refereeGuard")
public class RefereeGuard {

    private final RefereeSecurityProperties properties;

    public RefereeGuard(RefereeSecurityProperties properties) {
        this.properties = properties;
    }

    public boolean isReferee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String requiredAuthority = "ROLE_" + properties.refereeRole();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(requiredAuthority));
    }
}
