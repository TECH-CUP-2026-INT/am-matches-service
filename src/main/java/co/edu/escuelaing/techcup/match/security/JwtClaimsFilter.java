package co.edu.escuelaing.techcup.match.security;

import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * El API Gateway ya validó la firma y expiración del JWT; este filtro solo decodifica
 * los claims (sin volver a verificar la firma) para poblar el contexto de seguridad
 * con el id del usuario y sus roles, de forma que se pueda exigir el rol "arbitro".
 */
@Component
public class JwtClaimsFilter extends OncePerRequestFilter {

    private final RefereeSecurityProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtClaimsFilter(RefereeSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String tokenHeader = request.getHeader("X-Auth-Token");
        
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (tokenHeader != null && !tokenHeader.isBlank()) {
            token = tokenHeader;
        }
        
        if (token != null) {
            try {
                authenticate(token);
            } catch (Exception ex) {
                logger.warn("No fue posible interpretar el JWT recibido: " + ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) throws IOException {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return;
        }

        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {
        });

        Object subject = claims.get("sub");
        if (subject == null) {
            return;
        }

        UUID userId = UUID.fromString(subject.toString());
        Set<String> roles = extractRoles(claims.get(properties.roleClaim()));
        AuthenticatedReferee principal = new AuthenticatedReferee(userId, roles);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Set<String> extractRoles(Object rawRoles) {
        if (rawRoles instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).collect(Collectors.toSet());
        }
        if (rawRoles instanceof String single) {
            return Set.of(single);
        }
        return Set.of();
    }
}
