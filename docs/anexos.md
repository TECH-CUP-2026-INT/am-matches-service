# Anexos

## Glosario

| Término | Significado |
|---|---|
| Árbitro | Único actor humano de este servicio; gestiona el partido en vivo |
| Planilla / acta | Documento (PDF/imagen) que resume oficialmente el partido |
| Tiempo añadido | Minutos de descuento agregados a un periodo |
| Sanción | Consecuencia disciplinaria por acumulación de amarillas o roja directa |
| `eventType` | Código explícito de un evento de partido, independiente del color |
| `colorHint` | Sugerencia visual de color para un evento; nunca fuente de verdad |
| Best-effort | Llamada a otro servicio cuyo fallo se registra pero no bloquea el flujo principal |
| Puerto (arquitectura) | Interfaz de dominio que abstrae una integración externa |

## Hallazgos de seguridad

Hallazgos corregidos durante la revisión de seguridad del servicio:

- **Path traversal en la subida de planilla**: `LocalFileStorage` sanitizaba
  el nombre de archivo reemplazando caracteres fuera de un allowlist, pero
  dejaba pasar `.` y `..`, y un nombre de archivo literal `".."` escapaba un
  nivel de directorio (`Path.resolve("..")` apunta al padre). Corregido: se
  normaliza la ruta resultante y se verifica que siga contenida dentro del
  directorio del partido; nombres compuestos solo por puntos se reemplazan
  por un nombre fijo.
- **Sin límite de tamaño ni tipo de archivo en la planilla**: cualquier tipo
  y tamaño de archivo se aceptaba. Ahora hay un límite de 10MB
  (`spring.servlet.multipart.max-file-size`) y un allowlist de
  content-types (PDF, JPEG, PNG).
- **Warning de credencial en memoria generada por Spring Boot**: al no haber
  `UserDetailsService` propio, Spring Boot generaba y logueaba una
  contraseña de desarrollo en cada arranque. Esa autoconfiguración no aplica
  aquí (la autenticación real la resuelve `JwtClaimsFilter`), así que se
  excluyó explícitamente.

**Implicación operativa (no negociable):** como este servicio confía
ciegamente en que el JWT ya fue validado, **nunca debe exponerse directo a
internet ni a otros servicios que no sea el API Gateway** — cualquiera que le
hable directamente puede fabricar un token con cualquier `sub`/`roles` y
pasar la autorización. Debe protegerse a nivel de red (firewall/security
group/service mesh) para que solo el Gateway pueda alcanzar su puerto.

Puntos que quedan fuera del alcance de este servicio, por diseño
(responsabilidad de la infraestructura/Gateway): HTTPS/TLS, rate limiting, y
CORS — todos se resuelven en el borde de la plataforma, no en cada
microservicio individual.

## Pipeline de CI/CD

Definido en [`.github/workflows/ci.yml`](https://github.com/TECH-CUP-2026-INT/am-matches-service/blob/main/.github/workflows/ci.yml),
se dispara en cada `push` a `main`, `develop` y `feature/**`, y en cada
`pull_request` hacia `main`/`develop`. Etapas:

1. **Checkout** del código (`actions/checkout`).
2. **Configuración del entorno**: JDK 21 (Temurin) con cache de dependencias
   Maven.
3. **Compilación** (`./mvnw compile`).
4. **Ejecución de pruebas** (`./mvnw test`), con publicación del reporte de
   Surefire como artefacto.
5. **Análisis estático** con SonarQube (`./mvnw sonar:sonar`), omitido en
   pull requests desde forks (no tienen acceso a los secrets).
6. **Empaquetado** del JAR (`./mvnw package -DskipTests`, las pruebas ya se
   corrieron en el paso 4) y publicación como artefacto de GitHub Actions.
7. **Dockerización**: build de la imagen con el `Dockerfile` multi-stage del
   repositorio, solo en eventos `push`.
8. **Publicación de artefactos**: JAR publicado como artefacto del workflow;
   imagen Docker publicada en GitHub Container Registry
   (`ghcr.io/tech-cup-2026-int/am-matches-service`), etiquetada con
   `latest` (rama por defecto), nombre de rama y SHA corto del commit.

## Referencias

- [MkDocs](https://www.mkdocs.org/) — generador de sitios de documentación
  estática usado en este proyecto.
- [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) — tema
  usado para el sitio.
- [Spring Boot](https://spring.io/projects/spring-boot) — framework del
  servicio.
- [Flyway](https://flywaydb.org/) — herramienta de migraciones de esquema.
- [springdoc-openapi](https://springdoc.org/) — generación de la
  especificación OpenAPI y Swagger UI.

## Historial de cambios

| Fecha | Cambio |
|---|---|
| 2026-07-09 | Documentación técnica migrada a MkDocs; pipeline de CI/CD con GitHub Actions añadido (build, test, análisis estático, empaquetado, Docker, publicación de artefactos). |
| 2026-07-09 | Restaurado el paquete `storage` (`FileStorage` + `LocalFileStorage`) que faltaba en el código fuente y rompía la compilación; implementa la sanitización de rutas descrita en [Hallazgos de seguridad](#hallazgos-de-seguridad). |
