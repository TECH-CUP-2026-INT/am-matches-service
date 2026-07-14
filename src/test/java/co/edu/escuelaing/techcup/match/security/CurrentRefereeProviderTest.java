package co.edu.escuelaing.techcup.match.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentRefereeProviderTest {

    private final CurrentRefereeProvider provider = new CurrentRefereeProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthentication_throws() {
        SecurityContextHolder.clearContext();
        assertThrows(InsufficientAuthenticationException.class, provider::getCurrentRefereeId);
    }

    @Test
    void principalNotAuthenticatedReferee_throws() {
        var authentication = new UsernamePasswordAuthenticationToken("notAReferee", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(InsufficientAuthenticationException.class, provider::getCurrentRefereeId);
    }

    @Test
    void authenticatedReferee_returnsUserId() {
        UUID userId = UUID.randomUUID();
        var principal = new AuthenticatedReferee(userId, Set.of("ARBITRO"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(provider.getCurrentRefereeId()).isEqualTo(userId);
    }
}
