package co.edu.escuelaing.techcup.match.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminOrOrganizadorGuardTest {

    private final AdminOrOrganizadorGuard guard = new AdminOrOrganizadorGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthentication_returnsFalse() {
        SecurityContextHolder.clearContext();
        assertThat(guard.isAdminOrOrganizador()).isFalse();
    }

    @Test
    void authenticatedWithAdminRole_returnsTrue() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isAdminOrOrganizador()).isTrue();
    }

    @Test
    void authenticatedWithOrganizadorRole_returnsTrue() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "organizador", null, List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isAdminOrOrganizador()).isTrue();
    }

    @Test
    void authenticatedWithUnrelatedRole_returnsFalse() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "arbitro", null, List.of(new SimpleGrantedAuthority("ROLE_ARBITRO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(guard.isAdminOrOrganizador()).isFalse();
    }
}
