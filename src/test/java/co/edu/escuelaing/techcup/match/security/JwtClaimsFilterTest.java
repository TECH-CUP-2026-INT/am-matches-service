package co.edu.escuelaing.techcup.match.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtClaimsFilterTest {

    private final RefereeSecurityProperties properties = new RefereeSecurityProperties("roles", "ARBITRO");
    private final JwtClaimsFilter filter = new JwtClaimsFilter(properties);

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String buildToken(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    @Test
    void validTokenWithRolesArray_populatesSecurityContext() throws Exception {
        String userId = "b3b6a1f0-1111-4a2b-9c3d-000000000001";
        String token = buildToken("{\"sub\":\"" + userId + "\",\"roles\":[\"ARBITRO\",\"OTRO\"]}");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        AuthenticatedReferee principal = (AuthenticatedReferee) authentication.getPrincipal();
        assertThat(principal.userId()).hasToString(userId);
        assertThat(principal.roles()).containsExactlyInAnyOrder("ARBITRO", "OTRO");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ARBITRO", "ROLE_OTRO");
        verify(chain).doFilter(request, response);
    }

    @Test
    void validTokenWithSingleRoleString_populatesSecurityContext() throws Exception {
        String userId = "b3b6a1f0-1111-4a2b-9c3d-000000000002";
        String token = buildToken("{\"sub\":\"" + userId + "\",\"roles\":\"ARBITRO\"}");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(Object::toString).contains("ROLE_ARBITRO");
    }

    @Test
    void tokenWithoutRolesClaim_resultsInNoAuthorities() throws Exception {
        String userId = "b3b6a1f0-1111-4a2b-9c3d-000000000003";
        String token = buildToken("{\"sub\":\"" + userId + "\"}");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaders")
    void invalidAuthorizationHeader_doesNotAuthenticate(String headerValue) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(headerValue);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    private static Stream<String> invalidAuthorizationHeaders() {
        return Stream.of(null, "Basic abc123", "Bearer onlyoneparthere");
    }

    @Test
    void malformedToken_isSwallowedAndChainContinues() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer not-a-valid-jwt-!!!");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void payloadWithInvalidBase64_isCaughtAndChainContinues() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        // Dos partes separadas por punto (pasa el chequeo de longitud mínima), pero el
        // payload no es Base64 válido: Base64.getUrlDecoder().decode lanza IllegalArgumentException,
        // que debe quedar atrapada por el catch genérico del filtro.
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer header.!!!not-valid-base64!!!");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenWithoutSubjectClaim_doesNotAuthenticate() throws Exception {
        String token = buildToken("{\"roles\":[\"ARBITRO\"]}");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
