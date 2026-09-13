---
title: Instal·lació
linkTitle: Instal·lació
weight: 10
---

Fanel es distribueix com una única imatge Docker que serveix l'API i el
client web des del mateix procés. Necessites `docker` i `docker compose`.

## Amb PostgreSQL (recomanat per a ús continuat)

```sh
git clone https://github.com/giro-dev/fanel.git
cd fanel
cp deploy/.env.example deploy/.env
# edita deploy/.env: contrasenyes, orígens CORS, etc.
docker compose -f deploy/docker-compose.yml up --build -d
```

## Amb SQLite (més senzill, un únic fitxer de dades)

```sh
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.sqlite.yml up --build -d
```

En tots dos casos l'aplicació queda disponible a <http://localhost:8080>.

## Versions publicades

Cada release (`vX.Y.Z`) publica:

- Una imatge Docker multi-arquitectura a `ghcr.io/giro-dev/fanel:vX.Y.Z`
  (i `:latest` per a l'última versió de `main`).
- Un APK sense signar per a Android com a adjunt de la
  [release a GitHub](https://github.com/giro-dev/fanel/releases).

Per fixar una versió concreta enlloc de `latest`, edita la imatge al
`docker-compose.yml`:

```yaml
services:
  app:
    image: ghcr.io/giro-dev/fanel:v0.2.0
```

## Primer accés

En perfil `dev` hi ha un usuari `admin` / `admin` (canvia la contrasenya a
`.env` amb `FANEL_ADMIN_USER` / `FANEL_ADMIN_PASSWORD` abans de posar-ho en
producció). Un cop dins, cada membre adult de la llar pot configurar el seu
propi usuari i contrasenya des de "Compte".
