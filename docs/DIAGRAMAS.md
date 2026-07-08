# Diagramas — Servicio de Partidos

## 1. Componentes generales

Vista de alto nivel: el árbitro nunca llega directo a este servicio (todo pasa por
el API Gateway), y este servicio nunca llama directo a otro microservicio de
negocio salvo a través de sus puertos de integración.

```mermaid
flowchart TB
    Arbitro["Árbitro<br/>(app móvil/web)"]
    Gateway["API Gateway<br/>(valida firma y expiración del JWT)"]

    subgraph SM["Servicio de Partidos"]
        direction TB
        Controller["Controllers REST<br/>(match / goal / card / substitution / observation / sheet)"]
        Security["JwtClaimsFilter + RefereeGuard<br/>(decodifica claims, exige rol ARBITRO)"]
        Service["Capa de Servicio<br/>(reglas de negocio: cronómetro, marcador, sanciones)"]
        Repo["Repositorios JPA"]
        Ports["Puertos de integración<br/>(interfaces)"]

        Controller --> Security
        Security --> Service
        Service --> Repo
        Service --> Ports
    end

    DB[("PostgreSQL<br/>techcup_matches")]
    Competencia["Servicio de Competencia"]
    Estadisticas["Servicio de Estadísticas"]
    Notificaciones["Servicio de Notificaciones"]
    Auditoria["Servicio de Auditoría"]

    Arbitro -->|HTTPS + JWT| Gateway
    Gateway -->|HTTPS + JWT ya validado| Controller
    Repo --> DB
    Ports -->|"REST síncrono (precondición bloqueante)"| Competencia
    Ports -->|"REST síncrono best-effort"| Estadisticas
    Ports -->|"REST síncrono best-effort"| Notificaciones
    Ports -->|"REST síncrono best-effort"| Auditoria
```

**Por qué el Gateway está en el camino crítico:** este servicio decodifica los
claims del JWT pero **no verifica su firma** — asume que solo el Gateway puede
alcanzarlo. Por eso no debe exponerse directo a internet (ver [Seguridad](#seguridad)
en el README).

---

## 2. Clases (modelo de dominio)

```mermaid
classDiagram
    class Match {
        UUID id
        UUID competenciaMatchId
        UUID homeTeamId
        UUID awayTeamId
        String homeTeamName
        String awayTeamName
        UUID refereeId
        MatchStatus status
        MatchPeriod currentPeriod
        int homeScore
        int awayScore
        int addedMinutesFirstHalf
        int addedMinutesSecondHalf
        Instant periodStartedAt
        long accumulatedSeconds
        Instant startedAt
        Instant endedAt
    }

    class Goal {
        UUID id
        UUID teamId
        UUID playerId
        int minute
        MatchPeriod period
        Instant createdAt
    }

    class Card {
        UUID id
        UUID teamId
        UUID playerId
        CardType cardType
        int minute
        MatchPeriod period
        Instant createdAt
    }

    class Substitution {
        UUID id
        UUID teamId
        UUID playerOutId
        UUID playerInId
        int minute
        MatchPeriod period
        Instant createdAt
    }

    class MatchObservation {
        UUID id
        UUID refereeId
        String text
        Integer minute
        Instant createdAt
    }

    class MatchSheet {
        UUID id
        String fileUrl
        UUID uploadedBy
        Instant uploadedAt
    }

    class MatchStatus {
        <<enumeration>>
        SCHEDULED
        IN_PROGRESS
        PAUSED
        FINISHED
    }

    class MatchPeriod {
        <<enumeration>>
        FIRST_HALF
        SECOND_HALF
    }

    class CardType {
        <<enumeration>>
        YELLOW
        RED
    }

    class EventType {
        <<enumeration>>
        MATCH_STARTED
        MATCH_PAUSED
        MATCH_RESUMED
        MATCH_FINISHED
        GOAL
        YELLOW_CARD
        RED_CARD
        SUBSTITUTION
        OBSERVATION
        PLAYER_SANCTIONED
    }

    Match "1" --> "0..*" Goal : registra
    Match "1" --> "0..*" Card : registra
    Match "1" --> "0..*" Substitution : registra
    Match "1" --> "0..*" MatchObservation : registra
    Match "1" --> "0..1" MatchSheet : tiene

    Match --> MatchStatus
    Match --> MatchPeriod
    Card --> CardType
    Goal --> MatchPeriod
    Card --> MatchPeriod
    Substitution --> MatchPeriod
```

`EventType` no es una columna persistida en estas entidades: es el código que
cada `*Response` DTO expone junto al color sugerido, para que ninguna alerta
dependa solo de color (accesibilidad daltonismo).

---

## 3. Secuencia: iniciar partido

Precondición bloqueante: si Competencia no confirma la alineación o el horario
programado no ha llegado, el partido no inicia.

```mermaid
sequenceDiagram
    actor Arbitro as Árbitro
    participant GW as API Gateway
    participant MC as MatchController
    participant MS as MatchService
    participant CC as CompetenciaClient
    participant Comp as Servicio de Competencia
    participant DB as PostgreSQL
    participant Aud as AuditReporter

    Arbitro->>GW: POST /partidos/{competenciaMatchId}/iniciar (JWT)
    GW->>MC: request + JWT ya validado
    MC->>MS: startMatch(competenciaMatchId, refereeId)
    MS->>CC: getScheduledMatch(competenciaMatchId)
    CC->>Comp: GET /api/partidos/{id}
    Comp-->>CC: alineación confirmada + hora programada
    CC-->>MS: ScheduledMatchInfo

    alt alineación no confirmada u hora no alcanzada
        MS-->>MC: MatchNotReadyException
        MC-->>Arbitro: 409 Conflict
    else listo para iniciar
        MS->>DB: crear/actualizar Match (status=IN_PROGRESS)
        DB-->>MS: Match persistido
        MS->>Aud: report(MATCH_STARTED)
        Aud-->>MS: (best-effort, no bloquea)
        MS-->>MC: MatchResponse
        MC-->>Arbitro: 201 Created
    end
```

---

## 4. Secuencia: registrar tarjeta (con regla de sanción)

```mermaid
sequenceDiagram
    actor Arbitro as Árbitro
    participant CardC as CardController
    participant CardS as CardService
    participant DB as PostgreSQL
    participant Est as Servicio de Estadísticas
    participant Aud as Servicio de Auditoría
    participant Not as Servicio de Notificaciones

    Arbitro->>CardC: POST /partidos/{id}/tarjetas {teamId, playerId, cardType}
    CardC->>CardS: registerCard(matchId, refereeId, request)
    CardS->>DB: validar partido activo + equipo pertenece al partido
    CardS->>DB: guardar Card
    DB-->>CardS: Card persistida
    CardS->>Est: publishCard(evento) [best-effort]
    CardS->>Aud: report(YELLOW_CARD | RED_CARD) [best-effort]

    alt tarjeta ROJA
        CardS->>Not: notifyPlayerSanctioned() [best-effort]
    else tarjeta AMARILLA
        CardS->>DB: contar amarillas del jugador en este partido
        DB-->>CardS: conteo
        alt conteo >= umbral configurable (default 2)
            CardS->>Not: notifyPlayerSanctioned() [best-effort]
        else por debajo del umbral
            Note over CardS: no se notifica sanción
        end
    end

    CardS-->>CardC: CardResponse {eventType, colorHint, playerSanctioned}
    CardC-->>Arbitro: 201 Created
```

---

## 5. Secuencia: registrar gol

```mermaid
sequenceDiagram
    actor Arbitro as Árbitro
    participant GoalC as GoalController
    participant GoalS as GoalService
    participant DB as PostgreSQL
    participant Est as Servicio de Estadísticas
    participant Aud as Servicio de Auditoría

    Arbitro->>GoalC: POST /partidos/{id}/goles {teamId, playerId, minute}
    GoalC->>GoalS: registerGoal(matchId, refereeId, request)
    GoalS->>DB: validar partido activo + equipo pertenece al partido
    GoalS->>DB: guardar Goal + incrementar marcador del equipo
    DB-->>GoalS: Goal persistido, marcador actualizado
    GoalS->>Est: publishGoal(evento) [best-effort]
    GoalS->>Aud: report(GOAL) [best-effort]
    GoalS-->>GoalC: GoalResponse {homeScore, awayScore, eventType=GOAL}
    GoalC-->>Arbitro: 201 Created
```
