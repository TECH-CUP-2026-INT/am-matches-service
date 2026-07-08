package co.edu.escuelaing.techcup.match.integration.notificaciones;

/**
 * Puerto hacia el Servicio de Notificaciones. Se invoca cuando un jugador cruza
 * el umbral configurable de tarjetas (techcup.sanciones.umbral-amarillas-partido) o recibe roja directa.
 */
public interface SanctionNotifier {

    void notifyPlayerSanctioned(PlayerSanctionedPayload payload);
}
