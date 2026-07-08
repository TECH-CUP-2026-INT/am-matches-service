package co.edu.escuelaing.techcup.match.mapper;

import co.edu.escuelaing.techcup.match.dto.response.CardResponse;
import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;

public final class CardMapper {

    private CardMapper() {
    }

    public static CardResponse toResponse(Card card, boolean playerSanctioned) {
        return new CardResponse(
                card.getId(),
                card.getMatch().getId(),
                card.getTeamId(),
                card.getPlayerId(),
                card.getCardType(),
                colorHint(card.getCardType()),
                card.getMinute(),
                card.getPeriod(),
                card.getCardType() == CardType.RED ? EventType.RED_CARD : EventType.YELLOW_CARD,
                playerSanctioned,
                card.getCreatedAt()
        );
    }

    private static String colorHint(CardType cardType) {
        return cardType == CardType.RED ? "rojo" : "amarillo";
    }
}
