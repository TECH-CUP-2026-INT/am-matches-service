package co.edu.escuelaing.techcup.match.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ServiceGuardTest {

    private final ServiceGuard guard = new ServiceGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthentication_returnsFalse() {
        SecurityContextHolder.clearContext();
        assertThat(guard.isService()).isFalse();
    }

    @Test
    void authenticatedWithServiceRole_returnsTrue() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isService()).isTrue();
    }

    @Test
    void authenticatedWithoutServiceRole_returnsFalse() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "arbitro", null, List.of(new SimpleGrantedAuthority("ROLE_ARBITRO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isService()).isFalse();
    }
}
