package co.edu.escuelaing.techcup.match.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.escuelaing.techcup.match.config.RabbitMQConfig;
import co.edu.escuelaing.techcup.match.entity.enums.MatchResult;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitMatchStatEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMatchStatEventPublisher publisher = new RabbitMatchStatEventPublisher(rabbitTemplate);

    private MatchStatEvent sampleEvent() {
        return new MatchStatEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                MatchResult.WON, 1, 0, 0, 0, 0, 0, false, LocalDateTime.now());
    }

    @Test
    void publish_sendsToTheSharedExchangeWithTheStatRoutingKey() {
        MatchStatEvent event = sampleEvent();

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.TECHCUP_EXCHANGE, "techcup.match.event.stat", event);
    }

    @Test
    void publish_whenTheBrokerFails_doesNotPropagateTheException() {
        doThrow(new AmqpException("connection refused")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.publish(sampleEvent())).doesNotThrowAnyException();
    }
}
