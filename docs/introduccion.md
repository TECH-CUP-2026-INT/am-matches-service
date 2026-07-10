# Introducción

## Contexto

**TechCup Fútbol** es un torneo universitario cuya plataforma digital, **Astro
Merge**, está compuesta por alrededor de 12 microservicios independientes,
cada uno responsable de un dominio de negocio acotado (competencia,
estadísticas, notificaciones, auditoría, partidos, etc.).

El **Servicio de Partidos** (`service-match`) es el microservicio encargado del
**arbitraje en tiempo real**: todo lo que ocurre desde que un árbitro abre un
partido asignado hasta que lo cierra al finalizar el encuentro.

## Propósito

Darle al árbitro una herramienta confiable para registrar en vivo, y sin
ambigüedad, todo lo que sucede en la cancha, de forma que:

- El marcador y el estado del partido reflejen la realidad en todo momento.
- Los eventos (goles, tarjetas, sustituciones) queden trazados con su minuto
  exacto y su jugador/equipo asociado.
- Las sanciones por acumulación de tarjetas se calculen automáticamente y se
  notifiquen sin intervención manual.
- Otros servicios de la plataforma (Estadísticas, Notificaciones, Auditoría)
  se mantengan sincronizados con lo que pasa en el partido, sin bloquear al
  árbitro si alguno de ellos falla.

## Actor único: el árbitro

Este servicio tiene un solo tipo de usuario final: el **árbitro** autenticado
a través del API Gateway de la plataforma. No expone funcionalidad para
jugadores, entrenadores ni espectadores.

## Alcance

### Qué SÍ hace este servicio

1. Mostrar al árbitro sus partidos asignados y habilitar "gestionar partido"
   solo cuando el encuentro realmente puede iniciar (alineación confirmada +
   hora de inicio alcanzada).
2. Iniciar el partido y controlar su cronología: pausa, reanudación, paso al
   segundo tiempo y adición de tiempo extra.
3. Registrar goles (equipo + jugador anotador) actualizando el marcador en
   vivo.
4. Registrar tarjetas amarillas/rojas por jugador, acumulando sanciones.
5. Registrar sustituciones (jugador que sale/entra + minuto exacto).
6. Registrar observaciones de texto libre del árbitro.
7. Recibir la planilla/acta del partido.
8. Finalizar el partido, cerrando el registro de eventos.

### Qué NO hace (responsabilidad de otros servicios)

| Responsabilidad | Servicio dueño |
|---|---|
| Programación de partidos, horarios y canchas | Servicio de Competencia |
| Alineaciones/nóminas de equipos | Servicio de Competencia |
| Tabla de posiciones y estadísticas acumuladas del torneo | Servicio de Estadísticas |
| Envío de notificaciones push/email | Servicio de Notificaciones |
| Registro de auditoría centralizado | Servicio de Auditoría |
| Autenticación y validación de firma del JWT | API Gateway |

Ver [Arquitectura](arquitectura.md) para el detalle de cómo este servicio se
comunica con cada uno de ellos.
