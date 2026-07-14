# Configuración

## Clonar el repositorio

```bash
git clone https://github.com/TECH-CUP-2026-INT/am-matches-service.git
cd am-matches-service
```

## Ejecutar el servicio localmente

```bash
# 1. Levantar MongoDB (definido en docker-compose.yml)
docker compose up -d

# 2. Ejecutar el servicio (los índices se crean automáticamente al arrancar)
./mvnw spring-boot:run
```

El servicio queda disponible en `http://localhost:8080`.

## Ejecutar con Docker

El repositorio incluye un `Dockerfile` multi-stage (build con Maven + runtime
en JRE 21 sobre Alpine):

```bash
docker build -t am-matches-service:latest .
docker run --rm -p 8080:8080 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/techcup_matches \
  am-matches-service:latest
```

## Variables de entorno

| Variable | Valor por defecto | Uso |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/techcup_matches` | Cadena de conexión a MongoDB |
| `SERVER_PORT` | `8080` | Puerto HTTP del servicio |
| `COMPETENCIA_SERVICE_URL` | `http://localhost:8081` | Base URL del Servicio de Competencia |
| `ESTADISTICAS_SERVICE_URL` | `http://localhost:8082` | Base URL del Servicio de Estadísticas |
| `NOTIFICACIONES_SERVICE_URL` | `http://localhost:8083` | Base URL del Servicio de Notificaciones |
| `AUDITORIA_SERVICE_URL` | `http://localhost:8084` | Base URL del Servicio de Auditoría |
| `MATCH_SHEETS_DIR` | `./storage/match-sheets` | Directorio local donde se guardan las planillas subidas |

Estas variables se resuelven en `src/main/resources/application.yml`.

## Configuración adicional en `application.yml`

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `techcup.security.role-claim` | `roles` | Claim del JWT donde se busca el rol |
| `techcup.security.referee-role` | `ARBITRO` | Rol requerido para acceder a los endpoints |
| `techcup.sanciones.umbral-amarillas-partido` | `2` | Cantidad de amarillas de un jugador en el mismo partido que disparan sanción |
| `spring.servlet.multipart.max-file-size` | `10MB` | Tamaño máximo de la planilla subida |

## Documentación (MkDocs)

La documentación técnica de este servicio está construida con
[MkDocs](https://www.mkdocs.org/) y el tema
[Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).

### Instalación

```bash
python -m venv .venv
# Linux / macOS
source .venv/bin/activate
# Windows (PowerShell)
.venv\Scripts\Activate.ps1

pip install mkdocs-material
```

### Servir la documentación en local

Desde la raíz del repositorio (donde vive `mkdocs.yml`):

```bash
mkdocs serve
```

Esto levanta un servidor local (por defecto en
[http://127.0.0.1:8000](http://127.0.0.1:8000)) con recarga automática al
guardar cambios en `docs/`.

### Compilar el sitio estático

```bash
mkdocs build
```

Genera el sitio en `site/` (carpeta ignorada por git), lista para publicarse
como contenido estático (por ejemplo, GitHub Pages).

### Estructura de la documentación

```
proyecto/
│
├── docs/
│   ├── index.md
│   ├── introduccion.md
│   ├── requerimientos.md
│   ├── configuracion.md
│   ├── arquitectura.md
│   ├── api.md
│   ├── pruebas.md
│   ├── equipo.md
│   ├── anexos.md
│   └── assets/
│       ├── img/
│       ├── diagrams/
│       └── stylesheets/
│           └── extra.css
│
├── mkdocs.yml
├── src/
```

Los colores y tipografía del tema (paleta morado/dorado de TechCup) están
definidos en `docs/assets/stylesheets/extra.css` y declarados en `mkdocs.yml`
bajo `extra_css`.
