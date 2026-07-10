# Pruebas

## Cómo ejecutar las pruebas

```bash
# Pruebas unitarias de la lógica de negocio (no requieren base de datos)
./mvnw test -Dtest=CardServiceImplTest,GoalServiceImplTest,MatchServiceImplTest,MatchClockTest

# Suite completa, incluyendo el test de contexto de Spring (requiere Postgres corriendo)
./mvnw test
```

Para levantar la base de datos requerida por la suite completa:

```bash
docker compose up -d
```

## Qué cubren las pruebas unitarias

| Clase de prueba | Cubre |
|---|---|
| `MatchServiceImplTest` | Transiciones válidas/inválidas del cronómetro (iniciar, pausar, reanudar, finalizar), precondición de alineación/hora con Competencia |
| `MatchClockTest` | Cálculo del minuto actual a partir de `accumulated_seconds` y `period_started_at` |
| `GoalServiceImplTest` | Registro de goles y actualización del marcador en vivo |
| `CardServiceImplTest` | Regla de sanción por acumulación de tarjetas amarillas y roja directa |
| `SubstitutionServiceImplTest` | Registro de sustituciones |
| `MatchObservationServiceImplTest` | Registro de observaciones de texto libre |
| `MatchSheetServiceImplTest` | Sanitización de rutas de archivo en la subida de la planilla (path traversal) |
| `ServiceMatchApplicationTests` | Carga del contexto de Spring Boot (requiere PostgreSQL) |

## Pruebas en el pipeline de CI

El workflow de GitHub Actions (`.github/workflows/ci.yml`) ejecuta
`./mvnw test` en cada `push` y `pull_request` hacia `main`/`develop`/`feature/**`,
antes de correr el análisis estático y de empaquetar el JAR. Si alguna prueba
falla, el pipeline se detiene y no se genera ni el artefacto JAR ni la imagen
Docker. Los reportes de Surefire quedan publicados como artefacto
(`test-reports`) del workflow para inspección posterior.

Ver [Configuración](configuracion.md) para variables de entorno y
[Arquitectura](arquitectura.md) para el detalle de las reglas de negocio que
estas pruebas verifican.
