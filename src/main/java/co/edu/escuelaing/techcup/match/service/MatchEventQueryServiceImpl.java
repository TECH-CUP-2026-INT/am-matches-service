package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.response.MatchEventResponse;
import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import co.edu.escuelaing.techcup.match.entity.Substitution;
import co.edu.escuelaing.techcup.match.entity.enums.MatchEventType;
import co.edu.escuelaing.techcup.match.repository.CardRepository;
import co.edu.escuelaing.techcup.match.repository.GoalRepository;
import co.edu.escuelaing.techcup.match.repository.MatchObservationRepository;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import co.edu.escuelaing.techcup.match.repository.SubstitutionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchEventQueryServiceImpl implements MatchEventQueryService {

    private final MatchRepository matchRepository;
    private final GoalRepository goalRepository;
    private final CardRepository cardRepository;
    private final SubstitutionRepository substitutionRepository;
    private final MatchObservationRepository matchObservationRepository;

    public MatchEventQueryServiceImpl(MatchRepository matchRepository,
                                       GoalRepository goalRepository,
                                       CardRepository cardRepository,
                                       SubstitutionRepository substitutionRepository,
                                       MatchObservationRepository matchObservationRepository) {
        this.matchRepository = matchRepository;
        this.goalRepository = goalRepository;
        this.cardRepository = cardRepository;
        this.substitutionRepository = substitutionRepository;
        this.matchObservationRepository = matchObservationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchEventResponse> listEvents(UUID matchId) {
        List<Match> matches = matchId != null
                ? matchRepository.findById(matchId).map(List::of).orElseGet(List::of)
                : matchRepository.findAll();

        List<Goal> goals = matchId != null
                ? goalRepository.findByMatchIdOrderByMinuteAsc(matchId)
                : goalRepository.findAll();

        List<Card> cards = matchId != null
                ? cardRepository.findByMatchIdOrderByMinuteAsc(matchId)
                : cardRepository.findAll();

        List<Substitution> substitutions = matchId != null
                ? substitutionRepository.findByMatchIdOrderByMinuteAsc(matchId)
                : substitutionRepository.findAll();

        List<MatchObservation> observations = matchId != null
                ? matchObservationRepository.findByMatchIdOrderByCreatedAtAsc(matchId)
                : matchObservationRepository.findAll();

        List<MatchEventResponse> events = new ArrayList<>();

        for (Match match : matches) {
            if (match.getStartedAt() != null) {
                events.add(new MatchEventResponse(
                        MatchEventType.PARTIDO_INICIADO,
                        match.getId(),
                        match.getId(),
                        match.getRefereeId(),
                        match.getStartedAt(),
                        "Partido iniciado: " + match.getHomeTeamName() + " vs " + match.getAwayTeamName()));
            }
            if (match.getEndedAt() != null) {
                events.add(new MatchEventResponse(
                        MatchEventType.PARTIDO_FINALIZADO,
                        match.getId(),
                        match.getId(),
                        match.getRefereeId(),
                        match.getEndedAt(),
                        "Partido finalizado: " + match.getHomeScore() + "-" + match.getAwayScore()));
            }
        }

        for (Goal goal : goals) {
            events.add(new MatchEventResponse(
                    MatchEventType.GOL,
                    goal.getMatch().getId(),
                    goal.getId(),
                    goal.getPlayerId(),
                    goal.getCreatedAt(),
                    "Gol al minuto " + goal.getMinute() + " (" + goal.getPeriod() + ")"));
        }

        for (Card card : cards) {
            events.add(new MatchEventResponse(
                    MatchEventType.TARJETA,
                    card.getMatch().getId(),
                    card.getId(),
                    card.getPlayerId(),
                    card.getCreatedAt(),
                    card.getCardType() + " al minuto " + card.getMinute()));
        }

        for (Substitution substitution : substitutions) {
            events.add(new MatchEventResponse(
                    MatchEventType.SUSTITUCION,
                    substitution.getMatch().getId(),
                    substitution.getId(),
                    substitution.getPlayerInId(),
                    substitution.getCreatedAt(),
                    "Entra " + substitution.getPlayerInId() + ", sale " + substitution.getPlayerOutId()
                            + " al minuto " + substitution.getMinute()));
        }

        for (MatchObservation observation : observations) {
            events.add(new MatchEventResponse(
                    MatchEventType.OBSERVACION,
                    observation.getMatch().getId(),
                    observation.getId(),
                    observation.getRefereeId(),
                    observation.getCreatedAt(),
                    truncate(observation.getText())));
        }

        events.sort(Comparator.comparing(MatchEventResponse::timestamp).reversed());
        return events;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        int maxLength = 140;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
