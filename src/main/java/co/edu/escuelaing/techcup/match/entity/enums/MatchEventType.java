package co.edu.escuelaing.techcup.match.entity.enums;

/**
 * Tipo de evento expuesto por el audit log local de partidos (ver
 * {@code MatchEventQueryService}), consultable por los roles Admin y Organizador.
 */
public enum MatchEventType {
    PARTIDO_INICIADO,
    PARTIDO_FINALIZADO,
    GOL,
    TARJETA,
    SUSTITUCION,
    OBSERVACION
}
