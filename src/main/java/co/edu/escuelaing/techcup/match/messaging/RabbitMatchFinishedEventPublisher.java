package co.edu.escuelaing.techcup.match.messaging;

import co.edu.escuelaing.techcup.match.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMatchFinishedEventPublisher implements MatchFinishedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMatchFinishedEventPublisher.class);
    private static final String ROUTING_KEY = "techcup.match.finished";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMatchFinishedEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(MatchFinishedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.TECHCUP_EXCHANGE, ROUTING_KEY, event);
        } catch (RuntimeException ex) {
            log.warn("No se pudo publicar el resultado del partido '{}' hacia Tournament",
                    event.matchId(), ex);
        }
    }
}
