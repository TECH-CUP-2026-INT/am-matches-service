package co.edu.escuelaing.techcup.match.security;

import java.util.Set;
import java.util.UUID;

/**
 * Principal de seguridad construido a partir de los claims del JWT ya validado por el API Gateway.
 */
public record AuthenticatedReferee(UUID userId, Set<String> roles) {
}
