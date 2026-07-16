package co.edu.escuelaing.techcup.match.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conectividad al exchange compartido de RabbitMQ ({@code techcup.exchange}, topic;
 * mismo broker que ya usan Torneos y Estadísticas — ver docs/rabbitmq-integration.md del
 * Servicio de Estadísticas). Este servicio (Partidos / "Competencia" en esa doc) todavía
 * no publica nada: {@code techcup.match.event.stat} requiere datos por jugador que hoy no
 * se trackean acá (faltas, minutos jugados, asistencias, arquero, tournamentId). Este
 * config solo deja lista la conexión para cuando esos datos existan.
 */
@Configuration
public class RabbitMQConfig {

    public static final String TECHCUP_EXCHANGE = "techcup.exchange";

    @Bean
    public TopicExchange techcupExchange() {
        return new TopicExchange(TECHCUP_EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
