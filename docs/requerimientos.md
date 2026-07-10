# Requerimientos

## Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-01 | El sistema debe listar al árbitro autenticado únicamente los partidos que tiene asignados. |
| RF-02 | El sistema debe permitir iniciar un partido solo si el Servicio de Competencia confirma alineación y hora programada alcanzada. |
| RF-03 | El sistema debe permitir pausar, reanudar y finalizar el cronómetro del partido, así como pasar de primer a segundo tiempo. |
| RF-04 | El sistema debe permitir agregar tiempo añadido (descuento) por periodo. |
| RF-05 | El sistema debe registrar goles asociando equipo, jugador y minuto, y actualizar el marcador en vivo. |
| RF-06 | El sistema debe registrar tarjetas amarillas y rojas por jugador y aplicar la regla de sanción por acumulación. |
| RF-07 | El sistema debe registrar sustituciones indicando jugador que sale, jugador que entra y minuto. |
| RF-08 | El sistema debe permitir registrar observaciones de texto libre del árbitro. |
| RF-09 | El sistema debe permitir subir y consultar la planilla/acta del partido. |
| RF-10 | El sistema debe notificar al Servicio de Estadísticas y de Auditoría los eventos relevantes del partido, sin bloquear el flujo del árbitro si esa notificación falla. |
| RF-11 | El sistema debe notificar al Servicio de Notificaciones cuando un jugador queda sancionado (roja directa o umbral de amarillas alcanzado). |
| RF-12 | Cada evento de partido debe exponer un campo `eventType` explícito, no solo un color, para garantizar accesibilidad a usuarios con daltonismo. |

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
| [Docker](https://www.docker.com/) / Docker Compose | 24+ | Base de datos PostgreSQL y contenedor de la aplicación |
| [Git](https://git-scm.com/) | 2.x | Control de versiones |
| Maven Wrapper (`mvnw`, incluido en el repo) | — | No requiere instalación de Maven local |

Para trabajar en la documentación:

| Herramienta | Versión mínima | Uso |
|---|---|---|
| [Python](https://www.python.org/) | 3.9+ | Requerido por MkDocs |
| [MkDocs](https://www.mkdocs.org/) + [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) | — | Generación del sitio de documentación |

Ver [Configuración](configuracion.md) para los pasos de instalación de cada
herramienta y las variables de entorno del servicio.
