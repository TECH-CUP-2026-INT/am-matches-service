package co.edu.escuelaing.techcup.match.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.escuelaing.techcup.match.config.RabbitMQConfig;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitMatchFinishedEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMatchFinishedEventPublisher publisher = new RabbitMatchFinishedEventPublisher(rabbitTemplate);

    private MatchFinishedEvent sampleEvent() {
        return new MatchFinishedEvent(UUID.randomUUID(), UUID.randomUUID(), MatchPhase.GRUPOS,
                2, 1, UUID.randomUUID(), null, null, Instant.now());
    }

    @Test
    void publish_sendsToTheSharedExchangeWithTheFinishedRoutingKey() {
        MatchFinishedEvent event = sampleEvent();

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.TECHCUP_EXCHANGE, "techcup.match.finished", event);
    }

    @Test
    void publish_whenTheBrokerFails_doesNotPropagateTheException() {
        doThrow(new AmqpException("connection refused")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.publish(sampleEvent())).doesNotThrowAnyException();
    }
}
