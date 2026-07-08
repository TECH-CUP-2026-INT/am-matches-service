package co.edu.escuelaing.techcup.match.security;

import java.util.UUID;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentRefereeProvider {

    public UUID getCurrentRefereeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedReferee referee)) {
            throw new InsufficientAuthenticationException("No hay un árbitro autenticado en el contexto de seguridad");
        }
        return referee.userId();
    }
}
