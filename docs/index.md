# Servicio de Partidos (service-match)

Microservicio responsable del **arbitraje en tiempo real** de los partidos del
torneo universitario **TechCup Fútbol**. Es uno de ~12 microservicios
independientes de la plataforma Astro Merge; su único actor es el **árbitro** y
su única responsabilidad es la ejecución en vivo del partido.

[Ver en GitHub](https://github.com/TECH-CUP-2026-INT/am-matches-service){ .md-button .md-button--primary }
[Explorar la API](api.md){ .md-button }

## Mapa de la documentación

| Sección | Contenido |
|---|---|
| [Introducción](introduccion.md) | Contexto, propósito y alcance del servicio |
| [Requerimientos](requerimientos.md) | Requisitos funcionales, no funcionales y prerrequisitos técnicos |
| [Configuración](configuracion.md) | Variables de entorno, ejecución local y despliegue con Docker |
| [Arquitectura](arquitectura.md) | Capas, modelo de datos y diagramas (componentes, clases, secuencia) |
| [API](api.md) | Endpoints REST, autenticación y Swagger UI |
| [Pruebas](pruebas.md) | Estrategia de pruebas y cómo ejecutarlas |
| [Equipo](equipo.md) | Integrantes y roles del equipo TECH-CUP 2026 INT |
| [Anexos](anexos.md) | Glosario, hallazgos de seguridad y referencias |

## Resumen rápido

| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build | Maven |
| Persistencia | PostgreSQL + Spring Data JPA |
| Migraciones | Flyway |
| API | Spring Web (REST) + springdoc-openapi |
| Seguridad | Spring Security (verificación de rol; el JWT ya viene validado por el API Gateway) |
| CI/CD | GitHub Actions (build, test, análisis estático, empaquetado, Docker) |
| Documentación | MkDocs + Material for MkDocs |

## Inicio rápido

```bash
# 1. Levantar PostgreSQL
docker compose up -d

# 2. Ejecutar el servicio (Flyway crea el esquema automáticamente)
./mvnw spring-boot:run
```

Con el servicio corriendo, explora la API en
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

Para más detalle, ver [Configuración](configuracion.md) y [API](api.md).
