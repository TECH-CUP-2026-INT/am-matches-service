package co.edu.escuelaing.techcup.match.integration.auditoria;

/**
 * Puerto de auditoría, persistida localmente en este servicio (colección {@code audit_event}).
 * Debe invocarse para cada evento relevante: inicio, gol, tarjeta, sustitución y fin de partido.
 */
public interface AuditReporter {

    void report(MatchAuditEvent event);
}
