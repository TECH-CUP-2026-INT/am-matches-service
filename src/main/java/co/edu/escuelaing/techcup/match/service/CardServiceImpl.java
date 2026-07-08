package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.config.SanctionProperties;
import co.edu.escuelaing.techcup.match.dto.request.RegisterCardRequest;
import co.edu.escuelaing.techcup.match.dto.response.CardResponse;
import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.auditoria.MatchAuditEvent;
import co.edu.escuelaing.techcup.match.integration.estadisticas.CardEventPayload;
import co.edu.escuelaing.techcup.match.integration.estadisticas.MatchEventPublisher;
import co.edu.escuelaing.techcup.match.integration.notificaciones.PlayerSanctionedPayload;
import co.edu.escuelaing.techcup.match.integration.notificaciones.SanctionNotifier;
import co.edu.escuelaing.techcup.match.mapper.CardMapper;
import co.edu.escuelaing.techcup.match.repository.CardRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final MatchAccessService matchAccessService;
    private final MatchEventPublisher eventPublisher;
    private final SanctionNotifier sanctionNotifier;
    private final AuditReporter auditReporter;
    private final SanctionProperties sanctionProperties;

    public CardServiceImpl(CardRepository cardRepository,
                            MatchAccessService matchAccessService,
                            MatchEventPublisher eventPublisher,
                            SanctionNotifier sanctionNotifier,
                            AuditReporter auditReporter,
                            SanctionProperties sanctionProperties) {
        this.cardRepository = cardRepository;
        this.matchAccessService = matchAccessService;
        this.eventPublisher = eventPublisher;
        this.sanctionNotifier = sanctionNotifier;
        this.auditReporter = auditReporter;
        this.sanctionProperties = sanctionProperties;
    }

    @Override
    @Transactional
    public CardResponse registerCard(UUID matchId, UUID refereeId, RegisterCardRequest request) {
        Match match = matchAccessService.requireActiveMatch(matchId, refereeId);
        matchAccessService.validateTeamBelongsToMatch(match, request.teamId());

        int minute = request.minute() != null ? request.minute() : MatchClock.currentMinute(match);

        Card card = new Card();
        card.setMatch(match);
        card.setTeamId(request.teamId());
        card.setPlayerId(request.playerId());
        card.setCardType(request.cardType());
        card.setMinute(minute);
        card.setPeriod(match.getCurrentPeriod());
        card = cardRepository.save(card);

        eventPublisher.publishCard(new CardEventPayload(
                match.getId(), card.getTeamId(), card.getPlayerId(), card.getCardType(), minute));

        auditReporter.report(new MatchAuditEvent(
                match.getId(),
                card.getCardType() == CardType.RED ? EventType.RED_CARD : EventType.YELLOW_CARD,
                refereeId,
                Instant.now(),
                Map.of("teamId", card.getTeamId(), "playerId", card.getPlayerId(), "minute", minute)));

        boolean sanctioned = evaluateSanction(match, card);

        return CardMapper.toResponse(card, sanctioned);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> listCards(UUID matchId, UUID refereeId) {
        matchAccessService.requireOwnedMatch(matchId, refereeId);
        List<Card> cards = cardRepository.findByMatchIdOrderByMinuteAsc(matchId);

        Map<UUID, Integer> yellowCounts = new HashMap<>();
        List<CardResponse> responses = new ArrayList<>(cards.size());
        for (Card card : cards) {
            boolean sanctioned;
            if (card.getCardType() == CardType.RED) {
                sanctioned = true;
            } else {
                int count = yellowCounts.merge(card.getPlayerId(), 1, Integer::sum);
                sanctioned = count >= sanctionProperties.umbralAmarillasPartido();
            }
            responses.add(CardMapper.toResponse(card, sanctioned));
        }
        return responses;
    }

    /**
     * Regla de negocio confirmada: 2 tarjetas amarillas de un jugador en el mismo partido
     * equivalen a sanción; una roja directa sanciona de inmediato. El umbral de amarillas
     * es configurable vía techcup.sanciones.umbral-amarillas-partido.
     */
    private boolean evaluateSanction(Match match, Card card) {
        if (card.getCardType() == CardType.RED) {
            notifySanction(match, card, 0);
            return true;
        }

        long yellowCount = cardRepository.countByMatchIdAndPlayerIdAndCardType(
                match.getId(), card.getPlayerId(), CardType.YELLOW);

        if (yellowCount >= sanctionProperties.umbralAmarillasPartido()) {
            notifySanction(match, card, (int) yellowCount);
            return true;
        }
        return false;
    }

    private void notifySanction(Match match, Card card, int yellowCount) {
        sanctionNotifier.notifyPlayerSanctioned(new PlayerSanctionedPayload(
                match.getId(), card.getTeamId(), card.getPlayerId(), card.getCardType(), yellowCount, Instant.now()));
    }
}
