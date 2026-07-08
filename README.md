# Servicio de Partidos (service-match) — TechCup Fútbol

Microservicio responsable del **arbitraje en tiempo real** de los partidos del torneo
universitario TechCup Fútbol. Es uno de ~12 microservicios independientes de la
plataforma; su único actor es el **árbitro** y su única responsabilidad es la
ejecución en vivo del partido.

## Qué SÍ hace este servicio

1. Mostrar al árbitro sus partidos asignados y habilitar "gestionar partido" solo
   cuando el encuentro realmente puede iniciar (alineación confirmada + hora de
   inicio alcanzada).
2. Iniciar el partido y controlar su cronología: pausa, reanudación, paso al
   segundo tiempo y adición de tiempo extra.
3. Registrar goles (equipo + jugador anotador) actualizando el marcador en vivo.
4. Registrar tarjetas amarillas/rojas por jugador, acumulando sanciones.
5. Registrar sustituciones (jugador que sale/entra + minuto exacto).
6. Registrar observaciones de texto libre del árbitro.
7. Recibir la planilla/acta del partido.
8. Finalizar el partido, cerrando el registro de eventos.

## Qué NO hace (responsabilidad de otros servicios)

- Programación de partidos, horarios y canchas → **Servicio de Competencia**.
- Alineaciones/nóminas de equipos → **Servicio de Competencia**.
- Tabla de posiciones y estadísticas acumuladas del torneo → **Servicio de Estadísticas**.

## Stack técnico

| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build | Maven |
| Persistencia | PostgreSQL + Spring Data JPA |
| Migraciones | Flyway |
| API | Spring Web (REST) |
| Seguridad | Spring Security (verificación de rol; el JWT ya viene validado por el API Gateway) |
| Integraciones externas | REST síncrono vía `RestClient`, detrás de interfaces (puertos) |

## Arquitectura

Capas separadas, con la lógica de negocio desacoplada de los controllers y de los
detalles de integración externa:

```
controller/      Endpoints REST, validación de entrada, sin lógica de negocio
service/         Casos de uso e invariantes de negocio (interfaz + implementación)
entity/          Entidades JPA (persistencia)
repository/      Spring Data JPA
dto/request/     Payloads de entrada (records + Bean Validation)
dto/response/    Payloads de salida (siempre incluyen eventType, nunca solo color)
mapper/          entity <-> DTO
integration/     Puertos + adapters REST hacia Competencia, Estadísticas,
                 Notificaciones y Auditoría
storage/         Puerto + adapter de almacenamiento de la planilla del partido
security/        Lectura de claims del JWT y verificación del rol árbitro
exception/       Excepciones de dominio + manejador global (@RestControllerAdvice)
config/          Propiedades tipadas (@ConfigurationProperties) y seguridad
```

### Por qué integraciones síncronas detrás de puertos

Se evaluó mensajería asíncrona (Kafka/RabbitMQ) vs. llamadas REST síncronas para
notificar a Estadísticas, Notificaciones y Auditoría. Como la plataforma aún no
tiene un broker desplegado, se optó por el punto intermedio: **interfaces de
dominio** (`MatchEventPublisher`, `SanctionNotifier`, `AuditReporter`,
`CompetenciaClient`) con una única implementación REST configurable por URL. Esto
evita bloquear el desarrollo esperando infraestructura, y permite reemplazar la
implementación por un publisher de eventos sin tocar la capa de servicio.

Las llamadas hacia Estadísticas, Notificaciones y Auditoría son **best-effort**:
un fallo ahí se registra en el log pero nunca bloquea ni revierte el registro del
evento del árbitro (requisito de "responder rápido"). La llamada a Competencia
(obtener partido programado + alineación) sí es bloqueante, porque es una
precondición obligatoria para iniciar el partido.

## Modelo de datos

- **match**: estado del partido en vivo (marcador, periodo actual, cronómetro,
  tiempo añadido, referencia al partido programado en Competencia).
- **goal**, **card**, **substitution**: eventos del partido, cada uno ligado a
  `match`.
- **match_observation**: observaciones de texto libre del árbitro.
- **match_sheet**: referencia al archivo de la planilla subida (una por partido).

El cronómetro se calcula en el momento de la consulta a partir de
`accumulated_seconds` + `period_started_at` — no depende de un job en segundo
plano ni de columnas que haya que refrescar periódicamente.

## Regla de sanción por tarjetas

Configurable en `application.yml` (`techcup.sanciones.umbral-amarillas-partido`,
por defecto `2`): 2 tarjetas amarillas de un jugador en el mismo partido, o una
roja directa, disparan una notificación de sanción al Servicio de Notificaciones.

## Accesibilidad (daltonismo)

Todo evento de partido (gol, tarjeta, inicio, fin) expone un campo `eventType`
explícito en su respuesta. Las tarjetas además exponen `colorHint` como sugerencia
visual — nunca como única fuente de verdad.

## Seguridad

El API Gateway valida la firma y expiración del JWT antes de reenviar la
petición. Este servicio **no vuelve a verificar la firma**: decodifica los claims
del token (`JwtClaimsFilter`) para poblar el contexto de seguridad, y exige el rol
`arbitro` (configurable vía `techcup.security.referee-role`) con
`@PreAuthorize("@refereeGuard.isReferee()")` en cada endpoint.

**Implicación operativa (no negociable):** como este servicio confía ciegamente
en que el JWT ya fue validado, **nunca debe exponerse directo a internet ni a
otros servicios que no sea el API Gateway** — cualquiera que le hable
directamente puede fabricar un token con cualquier `sub`/`roles` y pasar la
autorización. Debe protegerse a nivel de red (firewall/security group/service
mesh) para que solo el Gateway pueda alcanzar su puerto.

Hallazgos corregidos durante la revisión de seguridad:

- **Path traversal en la subida de planilla**: `LocalFileStorage` sanitizaba el
  nombre de archivo reemplazando caracteres fuera de un allowlist, pero dejaba
  pasar `.` y `..`, y un nombre de archivo literal `".."` escapaba un nivel de
  directorio (`Path.resolve("..")` apunta al padre). Corregido: se normaliza la
  ruta resultante y se verifica que siga contenida dentro del directorio del
  partido; nombres compuestos solo por puntos se reemplazan por un nombre fijo.
- **Sin límite de tamaño ni tipo de archivo en la planilla**: cualquier tipo y
  tamaño de archivo se aceptaba. Ahora hay un límite de 10MB
  (`spring.servlet.multipart.max-file-size`) y un allowlist de content-types
  (PDF, JPEG, PNG).
- **Warning de credencial en memoria generada por Spring Boot**: al no haber
  `UserDetailsService` propio, Spring Boot generaba y logueaba una contraseña
  de desarrollo en cada arranque. Esa autoconfiguración no aplica aquí (la
  autenticación real la resuelve `JwtClaimsFilter`), así que se excluyó
  explícitamente.

Puntos que quedan fuera del alcance de este servicio, por diseño (responsabilidad
de la infraestructura/Gateway): HTTPS/TLS, rate limiting, y CORS — todos se
resuelven en el borde de la plataforma, no en cada microservicio individual.

`/swagger-ui/**` y `/v3/api-docs/**` están abiertos sin autenticación para
facilitar la exploración durante el desarrollo. Antes de un despliegue expuesto
más allá de la red interna, considerar restringirlos (perfil `dev` únicamente).

## Endpoints principales

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

## Configuración

Variables de entorno (con valor por defecto entre paréntesis):

| Variable | Uso |
|---|---|
| `DB_HOST` (`localhost`), `DB_PORT` (`5432`), `DB_NAME` (`techcup_matches`), `DB_USER` (`postgres`), `DB_PASSWORD` (`postgres`) | Conexión a PostgreSQL |
| `SERVER_PORT` (`8080`) | Puerto HTTP |
| `COMPETENCIA_SERVICE_URL` (`http://localhost:8081`) | Base URL del Servicio de Competencia |
| `ESTADISTICAS_SERVICE_URL` (`http://localhost:8082`) | Base URL del Servicio de Estadísticas |
| `NOTIFICACIONES_SERVICE_URL` (`http://localhost:8083`) | Base URL del Servicio de Notificaciones |
| `AUDITORIA_SERVICE_URL` (`http://localhost:8084`) | Base URL del Servicio de Auditoría |
| `MATCH_SHEETS_DIR` (`./storage/match-sheets`) | Directorio local donde se guardan las planillas subidas |

## Cómo ejecutar localmente

```bash
# 1. Levantar PostgreSQL
docker compose up -d

# 2. Ejecutar el servicio (Flyway crea el esquema automáticamente)
./mvnw spring-boot:run
```

## Explorar la API (Swagger UI)

Con el servicio corriendo: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Como el API Gateway (que valida el JWT) todavía no existe, para probar los endpoints
protegidos desde Swagger usa el botón **Authorize** con cualquier JWT bien formado
que incluya los claims `sub` (UUID) y `roles` (debe incluir `ARBITRO`) — no hace
falta que la firma sea válida, porque este servicio no la verifica (esa es
responsabilidad del Gateway en producción).

## Pruebas

```bash
# Pruebas unitarias de la lógica de negocio (no requieren base de datos)
./mvnw test -Dtest=CardServiceImplTest,GoalServiceImplTest,MatchServiceImplTest,MatchClockTest

# Suite completa, incluyendo el test de contexto de Spring (requiere Postgres corriendo)
./mvnw test
```

Las pruebas unitarias cubren la lógica de negocio central: la regla de sanción
por tarjetas, el marcador en vivo, las transiciones válidas/inválidas del
cronómetro (iniciar, pausar, reanudar, finalizar), el cálculo del minuto actual,
y la sanitización de rutas de archivo en la subida de planilla.

## Diagramas

Ver [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md): componentes generales, clases del
dominio, y secuencia de los tres flujos más representativos (iniciar partido,
registrar tarjeta con sanción, registrar gol).
