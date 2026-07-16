package co.edu.escuelaing.techcup.match.integration.torneos;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto hacia el Servicio de Torneos, para resolver el tournamentId de un partido
 * (necesario para publicar techcup.match.event.stat — ver Estadísticas
 * docs/rabbitmq-integration.md).
 */
public interface TournamentClient {

    Optional<UUID> findTournamentIdForMatch(UUID matchId);
}
