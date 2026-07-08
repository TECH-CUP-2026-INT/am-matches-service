package co.edu.escuelaing.techcup.match.entity.enums;

/**
 * Código de evento expuesto en toda respuesta/integración relacionada con el partido,
 * para que las alertas nunca dependan únicamente de un color (accesibilidad daltonismo).
 */
public enum EventType {
    MATCH_STARTED,
    MATCH_PAUSED,
    MATCH_RESUMED,
    MATCH_FINISHED,
    GOAL,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION,
    OBSERVATION,
    PLAYER_SANCTIONED
}
