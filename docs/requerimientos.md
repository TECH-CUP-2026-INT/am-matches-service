# Requerimientos

## Requisitos funcionales

| ID | Requisito | Estado |
|---|---|---|
| RF-01 | El sistema debe listar al árbitro autenticado únicamente los partidos que tiene asignados. | ✅ Implementado |
| RF-02 | El sistema debe permitir iniciar un partido solo si el Servicio de Competencia confirma alineación y hora programada alcanzada. | ✅ Implementado |
| RF-03 | El sistema debe permitir pausar, reanudar y finalizar el cronómetro del partido, así como pasar de primer a segundo tiempo. | ✅ Implementado |
| RF-04 | El sistema debe permitir agregar tiempo añadido (descuento) por periodo. | ✅ Implementado |
| RF-05 | El sistema debe registrar goles asociando equipo, jugador y minuto, y actualizar el marcador en vivo. | ✅ Implementado |
| RF-06 | El sistema debe registrar tarjetas amarillas y rojas por jugador y aplicar la regla de sanción por acumulación. | ✅ Implementado |
| RF-07 | El sistema debe registrar sustituciones indicando jugador que sale, jugador que entra y minuto. | ✅ Implementado |
| RF-08 | El sistema debe permitir registrar observaciones de texto libre del árbitro. | ✅ Implementado |
| RF-09 | El sistema debe permitir subir y consultar la planilla/acta del partido. | ✅ Implementado |
| RF-10 | El sistema debe notificar al Servicio de Estadísticas y de Auditoría los eventos relevantes del partido, sin bloquear el flujo del árbitro si esa notificación falla. | ✅ Implementado (best-effort) |
| RF-11 | El sistema debe notificar al Servicio de Notificaciones cuando un jugador queda sancionado (roja directa o umbral de amarillas alcanzado). | ✅ Implementado y **confirmado** end-to-end (incluye el header `X-Internal-Api-Key`, corregido en esta auditoría) |
| RF-12 | Cada evento de partido debe exponer un campo `eventType` explícito, no solo un color, para garantizar accesibilidad a usuarios con daltonismo. | ✅ Implementado |

## Cross-check contra la hoja de requerimientos del equipo (Excel, "Servicio de Partidos")

| Requisito de la hoja | Estado |
|---|---|
| Iniciar partido / registrar goles / tarjetas / sustituciones / observaciones / subir planilla (CREATE) | ✅ Implementado — ver RF-02, RF-05 a RF-09 |
| Controlar el desarrollo cronológico del partido — reloj (UPDATE) | ✅ Implementado — ver RF-03, RF-04 |
| Publicar / consultar calendario de partidos (READ, filtrable por equipo/fase/fecha) | ❌ No implementado en este servicio |
| Consultar los eventos del servicio de partidos — audit log, Admin y Organizador (READ) | ✅ Implementado — `GET /api/partidos/eventos`, ver [API](api.md#consultar-el-audit-log-de-eventos) |
| Editar partidos — cancha, árbitro, emparejamiento, fecha/hora (UPDATE) | ❌ No implementado en este servicio |
| Eliminar partido — solo si hay descalificación o error de emparejamiento (DELETE) | ❌ No implementado en este servicio |

!!! warning "Huecos funcionales detectados, con causa raíz identificada"
    Los tres huecos restantes (calendario, editar partido, eliminar partido) no son
    un olvido: el README de este servicio documenta explícitamente que la
    programación, el calendario y la edición/eliminación de partidos son
    responsabilidad del **Servicio de Competencia** (ver
    [Introducción](introduccion.md#qué-no-hace-responsabilidad-de-otros-servicios)),
    un servicio que **no existe todavía** en el workspace de los 3 repos de
    astromerge — es de otro equipo. `service-match` solo consume el partido ya
    programado (`CompetenciaClient.getScheduledMatch`) para poder iniciarlo, no
    gestiona su ciclo de vida administrativo. Ninguno de estos huecos es
    corregible sin acceso al código de esos otros servicios; quedan
    documentados aquí para que el equipo los priorice si la hoja de
    requerimientos espera que "Servicio de Partidos" los cubra directamente en
    vez de delegarlos.

    El hueco de audit log propio sí era corregible dentro de este servicio: se
    agregó `GET /api/partidos/eventos`, un audit log local de solo lectura que
    agrega goles, tarjetas, sustituciones, observaciones e inicio/fin de
    partido ya persistidos en la base de datos propia, accesible solo por los
    roles `ADMIN`/`ORGANIZADOR` (nuevos, ver [Arquitectura](arquitectura.md#roles-y-seguridad)).
    Esto es independiente del reporte best-effort al Servicio de Auditoría
    externo (RF-10), que sigue existiendo como una integración separada.

## Requisitos no funcionales

| ID | Requisito |
|---|---|
| RNF-01 | **Disponibilidad del flujo del árbitro**: los fallos en integraciones best-effort (Estadísticas, Notificaciones, Auditoría) no deben bloquear ni revertir el registro del evento. |
| RNF-02 | **Seguridad de red**: el servicio no verifica la firma del JWT (esa es responsabilidad del Gateway), por lo que debe permanecer inaccesible fuera de la red interna de la plataforma. |
| RNF-03 | **Trazabilidad**: cada evento de partido debe quedar persistido con marca de tiempo y, cuando aplique, minuto de juego. |
| RNF-04 | **Accesibilidad**: ninguna alerta o respuesta de la API debe depender únicamente del color para transmitir significado. |
| RNF-05 | **Integridad de archivos**: la planilla subida debe validarse por tamaño (máx. 10MB) y tipo de contenido (PDF, JPEG, PNG), y su ruta de almacenamiento debe sanitizarse contra path traversal. |
| RNF-06 | **Mantenibilidad**: la lógica de negocio debe estar desacoplada de los controllers REST y de los detalles de integración externa, detrás de interfaces (puertos). |
| RNF-07 | **Reproducibilidad del build**: el proyecto debe compilar, probar y empaquetarse de forma determinista vía Maven Wrapper, tanto en local como en CI. |
| RNF-08 | **Observabilidad operativa**: el servicio debe exponer endpoints de salud/métricas vía Spring Boot Actuator. |

## Prerrequisitos técnicos

Para desarrollar y ejecutar el servicio localmente:

| Herramienta | Versión mínima | Uso |
|---|---|---|
| [Java (JDK)](https://adoptium.net/) | 21 | Compilación y ejecución del servicio |
| [Docker](https://www.docker.com/) / Docker Compose | 24+ | Base de datos MongoDB y contenedor de la aplicación |
| [Git](https://git-scm.com/) | 2.x | Control de versiones |
| Maven Wrapper (`mvnw`, incluido en el repo) | — | No requiere instalación de Maven local |

Para trabajar en la documentación:

| Herramienta | Versión mínima | Uso |
|---|---|---|
| [Python](https://www.python.org/) | 3.9+ | Requerido por MkDocs |
| [MkDocs](https://www.mkdocs.org/) + [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) | — | Generación del sitio de documentación |

Ver [Configuración](configuracion.md) para los pasos de instalación de cada
herramienta y las variables de entorno del servicio.
