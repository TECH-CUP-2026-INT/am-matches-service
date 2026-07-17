package co.edu.escuelaing.techcup.match.messaging;

import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchResult;
import co.edu.escuelaing.techcup.match.repository.CardRepository;
import co.edu.escuelaing.techcup.match.repository.GoalRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Publica {@code techcup.match.event.stat} para cada jugador con al menos un gol o una
 * tarjeta registrada al finalizar un partido.
 *
 * <p><b>Limitación conocida:</b> esto no es "cada jugador" en sentido estricto — este
 * servicio nunca recibe la alineación completa del partido, solo la definición que
 * envía Tournament. Un jugador que jugó sin anotar ni ver tarjeta no genera evento,
 * porque no hay forma de saber que jugó.
 */
@Component
public class MatchFinishedStatPublisher {

    private final GoalRepository goalRepository;
    private final CardRepository cardRepository;
    private final MatchStatEventPublisher publisher;

    public MatchFinishedStatPublisher(GoalRepository goalRepository, CardRepository cardRepository,
            MatchStatEventPublisher publisher) {
        this.goalRepository = goalRepository;
        this.cardRepository = cardRepository;
        this.publisher = publisher;
    }

    public void publishStatsFor(Match match) {
        Map<UUID, PlayerAggregate> byPlayer = new LinkedHashMap<>();

        for (Goal goal : goalRepository.findByMatchIdOrderByMinuteAsc(match.getId())) {
            byPlayer.computeIfAbsent(goal.getPlayerId(), id -> new PlayerAggregate(goal.getTeamId())).goals++;
        }
        for (Card card : cardRepository.findByMatchIdOrderByMinuteAsc(match.getId())) {
            PlayerAggregate aggregate = byPlayer.computeIfAbsent(card.getPlayerId(), id -> new PlayerAggregate(card.getTeamId()));
            if (card.getCardType() == CardType.YELLOW) {
                aggregate.yellowCards++;
            } else {
                aggregate.redCards++;
            }
        }

        if (byPlayer.isEmpty()) {
            return;
        }

        UUID tournamentId = match.getTournamentId();
        LocalDateTime occurredAt = LocalDateTime.now(ZoneOffset.UTC);
        byPlayer.forEach((playerId, aggregate) -> publisher.publish(new MatchStatEvent(
                playerId, aggregate.teamId, match.getId(), tournamentId, resultFor(match, aggregate.teamId),
                aggregate.goals, aggregate.yellowCards, aggregate.redCards,
                0, 0, 0, false, occurredAt)));
    }

    private MatchResult resultFor(Match match, UUID teamId) {
        boolean isHomeTeam = teamId.equals(match.getHomeTeamId());
        int ownScore = isHomeTeam ? match.getHomeScore() : match.getAwayScore();
        int opponentScore = isHomeTeam ? match.getAwayScore() : match.getHomeScore();

        if (ownScore > opponentScore) {
            return MatchResult.WON;
        }
        if (ownScore < opponentScore) {
            return MatchResult.LOST;
        }
        return MatchResult.DRAWN;
    }

    private static final class PlayerAggregate {
        private final UUID teamId;
        private int goals;
        private int yellowCards;
        private int redCards;

        private PlayerAggregate(UUID teamId) {
            this.teamId = teamId;
        }
    }
}
