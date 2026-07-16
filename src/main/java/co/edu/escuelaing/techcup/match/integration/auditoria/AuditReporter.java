package co.edu.escuelaing.techcup.match.integration.auditoria;

/**
 * Puerto hacia el Servicio de Auditoría. Debe invocarse para cada evento relevante:
 * inicio, gol, tarjeta, sustitución y fin de partido.
 */
public interface AuditReporter {

    void report(MatchAuditEvent event);
}
