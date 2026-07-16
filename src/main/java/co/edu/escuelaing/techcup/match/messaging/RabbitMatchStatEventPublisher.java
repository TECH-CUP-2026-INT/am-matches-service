package co.edu.escuelaing.techcup.match.messaging;

import co.edu.escuelaing.techcup.match.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMatchStatEventPublisher implements MatchStatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMatchStatEventPublisher.class);
    private static final String ROUTING_KEY = "techcup.match.event.stat";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMatchStatEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(MatchStatEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.TECHCUP_EXCHANGE, ROUTING_KEY, event);
        } catch (RuntimeException ex) {
            log.warn("No se pudo publicar el evento de estadísticas del jugador '{}' en el partido '{}'",
                    event.playerId(), event.matchId(), ex);
        }
    }
}
