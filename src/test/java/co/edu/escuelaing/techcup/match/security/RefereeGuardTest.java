package co.edu.escuelaing.techcup.match.security;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RefereeGuardTest {

    private final RefereeSecurityProperties properties = new RefereeSecurityProperties("roles", "ARBITRO");
    private final RefereeGuard guard = new RefereeGuard(properties);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthentication_returnsFalse() {
        SecurityContextHolder.clearContext();
        assertThat(guard.isReferee()).isFalse();
    }

    @Test
    void authenticatedWithRefereeRole_returnsTrue() {
        var principal = new AuthenticatedReferee(UUID.randomUUID(), java.util.Set.of("ARBITRO"));
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ARBITRO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isReferee()).isTrue();
    }

    @Test
    void authenticatedWithoutRefereeRole_returnsFalse() {
        var principal = new AuthenticatedReferee(UUID.randomUUID(), java.util.Set.of("OTRO"));
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_OTRO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isReferee()).isFalse();
    }
}
