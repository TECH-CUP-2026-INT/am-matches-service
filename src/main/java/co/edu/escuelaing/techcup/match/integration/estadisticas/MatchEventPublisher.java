package co.edu.escuelaing.techcup.match.integration.estadisticas;

/**
 * Puerto hacia el Servicio de Estadísticas. No es crítico para el flujo del árbitro:
 * las implementaciones no deben propagar errores que bloqueen el registro del evento.
 */
public interface MatchEventPublisher {

    void publishGoal(GoalEventPayload payload);

    void publishCard(CardEventPayload payload);

    void publishSubstitution(SubstitutionEventPayload payload);
}
