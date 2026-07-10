# API

## Documentación interactiva (Swagger UI)

Con el servicio corriendo:
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

La especificación OpenAPI cruda está disponible en `/v3/api-docs`.

`/swagger-ui/**` y `/v3/api-docs/**` están abiertos sin autenticación para
facilitar la exploración durante el desarrollo. Antes de un despliegue
expuesto más allá de la red interna, considerar restringirlos (perfil `dev`
únicamente).

## Autenticación

El API Gateway valida la firma y expiración del JWT antes de reenviar la
petición. Este servicio **no vuelve a verificar la firma**: decodifica los
claims del token (`JwtClaimsFilter`) para poblar el contexto de seguridad, y
exige el rol `arbitro` (configurable vía `techcup.security.referee-role`) con
`@PreAuthorize("@refereeGuard.isReferee()")` en cada endpoint.

Como el API Gateway todavía no existe en el entorno de desarrollo, para
probar los endpoints protegidos desde Swagger usa el botón **Authorize** con
cualquier JWT bien formado que incluya los claims `sub` (UUID) y `roles`
(debe incluir `ARBITRO`) — no hace falta que la firma sea válida, porque este
servicio no la verifica (esa es responsabilidad del Gateway en producción).

## Endpoints

Todos bajo `/api/partidos`, requieren rol árbitro:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/partidos` | Partidos asignados al árbitro autenticado |
| GET | `/api/partidos/{matchId}` | Estado detallado de un partido |
| POST | `/api/partidos/{competenciaMatchId}/iniciar` | Inicia el partido (valida con Competencia) |
| POST | `/api/partidos/{matchId}/pausar` | Pausa el cronómetro |
| POST | `/api/partidos/{matchId}/reanudar` | Reanuda el cronómetro |
| POST | `/api/partidos/{matchId}/siguiente-tiempo` | Pasa de primer a segundo tiempo |
| POST | `/api/partidos/{matchId}/tiempo-adicional` | Agrega minutos de tiempo añadido |
| POST | `/api/partidos/{matchId}/finalizar` | Finaliza el partido |
| POST / GET | `/api/partidos/{matchId}/goles` | Registrar / listar goles |
| POST / GET | `/api/partidos/{matchId}/tarjetas` | Registrar / listar tarjetas |
| POST / GET | `/api/partidos/{matchId}/sustituciones` | Registrar / listar sustituciones |
| POST / GET | `/api/partidos/{matchId}/observaciones` | Registrar / listar observaciones |
| POST / GET | `/api/partidos/{matchId}/planilla` | Subir / consultar la planilla del partido |

## Ejemplo: registrar una tarjeta

`POST /api/partidos/{matchId}/tarjetas`

Request (`RegisterCardRequest`):

```json
{
  "teamId": "b2f1e2b0-7c1a-4e9a-9b1a-2f0e1c8d5a11",
  "playerId": "a1c3d4e5-1234-4a5b-8c9d-0e1f2a3b4c5d",
  "cardType": "YELLOW",
  "minute": 37
}
```

Response `201 Created` (`CardResponse`):

```json
{
  "id": "1f2e3d4c-5b6a-4978-8a9b-0c1d2e3f4a5b",
  "matchId": "9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d",
  "teamId": "b2f1e2b0-7c1a-4e9a-9b1a-2f0e1c8d5a11",
  "playerId": "a1c3d4e5-1234-4a5b-8c9d-0e1f2a3b4c5d",
  "cardType": "YELLOW",
  "colorHint": "#FBC02D",
  "minute": 37,
  "period": "SECOND_HALF",
  "eventType": "YELLOW_CARD",
  "playerSanctioned": true,
  "createdAt": "2026-07-09T20:12:45Z"
}
```

`colorHint` es solo una sugerencia visual para la UI; `eventType` (y
`cardType`) es la fuente de verdad — ninguna alerta debe depender únicamente
del color (accesibilidad daltonismo, ver [Anexos](anexos.md)).
`playerSanctioned` indica si esta tarjeta disparó una sanción (roja directa o
umbral de amarillas alcanzado, ver [Arquitectura](arquitectura.md#regla-de-sancion-por-tarjetas)).

## Errores

Los errores de negocio (partido no encontrado, transición de estado
inválida, partido no listo para iniciar, etc.) se manejan de forma
centralizada en `GlobalExceptionHandler` (`@RestControllerAdvice`) y se
devuelven con el DTO `ErrorResponse`, incluyendo un código HTTP acorde
(`404`, `409`, `400`, etc.).
