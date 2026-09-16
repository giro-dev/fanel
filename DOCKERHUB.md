# Fanel

**Fanel** (*the family organizer panel*) és una aplicació auto-allotjable per gestionar una llar: menú setmanal, receptes, llista de la compra, calendari i tasques, amb assistent d'IA i servidor MCP.

Una sola imatge serveix l'API REST (`/api/v1`) i la SPA (PWA) — cap servei addicional obligatori.

## Tags i arquitectures

- `latest` — darrera release publicada
- `X.Y.Z` — releases etiquetades (p. ex. `0.1.2`)
- Plataformes: `linux/amd64`, `linux/arm64`

## Inici ràpid (SQLite)

Sense cap dependència externa; la base de dades viu al volum `/data`:

```sh
docker run -d --name fanel \
  -p 8080:8080 \
  -v fanel-data:/data \
  -e SPRING_PROFILES_ACTIVE=sqlite \
  -e FANEL_ADMIN_PASSWORD=canvia-moi \
  ihipi/fanel:latest
```

Obre http://localhost:8080 — usuari `admin` i la contrasenya de `FANEL_ADMIN_PASSWORD`.

## docker-compose (SQLite)

```yaml
services:
  fanel:
    image: ihipi/fanel:latest
    environment:
      SPRING_PROFILES_ACTIVE: sqlite
      FANEL_ADMIN_PASSWORD: canvia-moi
    ports:
      - "8080:8080"
    volumes:
      - fanel-data:/data
    restart: unless-stopped

volumes:
  fanel-data:
```

## docker-compose (PostgreSQL)

```yaml
services:
  fanel:
    image: ihipi/fanel:latest
    environment:
      SPRING_PROFILES_ACTIVE: postgres
      FANEL_DB_URL: jdbc:postgresql://db:5432/fanel
      FANEL_DB_USER: fanel
      FANEL_DB_PASSWORD: canvia-moi
      FANEL_ADMIN_PASSWORD: canvia-moi
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:17
    environment:
      POSTGRES_DB: fanel
      POSTGRES_USER: fanel
      POSTGRES_PASSWORD: canvia-moi
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  postgres-data:
```

## Configuració (variables d'entorn)

| Variable | Per defecte | Descripció |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `postgres` | `postgres` o `sqlite` |
| `FANEL_ADMIN_USER` | `admin` | Usuari administrador inicial |
| `FANEL_ADMIN_PASSWORD` | `admin` | Contrasenya de l'admin — **canvia-la** |
| `FANEL_DB_URL` | `jdbc:postgresql://localhost:5432/fanel` | JDBC URL (perfil postgres) |
| `FANEL_DB_USER` / `FANEL_DB_PASSWORD` | `fanel` / `fanel` | Credencials Postgres |
| `FANEL_SQLITE_PATH` | `/data/fanel.db` | Ruta de la BD SQLite dins el contenidor |
| `FANEL_CORS_ORIGINS` | `http://localhost:5173` | Orígens permesos (llista separada per comes) |
| `FANEL_API_TOKEN` | *(buit)* | Token d'API opcional per a clients externs |

## Volums i ports

- `/data` — base de dades SQLite (només perfil `sqlite`)
- `8080` — API + SPA

## Enllaços

- Codi font i documentació: https://github.com/giro-dev/fanel
- Compose complets i `.env.example`: [`deploy/`](https://github.com/giro-dev/fanel/tree/main/deploy)
