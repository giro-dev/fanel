 # Fanel

Fanel és el panell d'organització familiar: una aplicació modular per coordinar
àpats, calendari, compres i tasques de la llar.

## Inici ràpid

Per executar amb PostgreSQL:

```sh
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.yml up --build
```

Per executar amb SQLite:

```sh
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.sqlite.yml up --build
```

L'aplicació queda disponible a <http://localhost:8080> (`admin` / `admin` només
en el perfil `dev`; canvia les credencials a `.env`). Un cop dins, cada membre
adult pot configurar el seu propi usuari i contrasenya des de "Compte" i deixar
d'utilitzar l'admin global pel dia a dia.

## Desenvolupament

Cal JDK 25, Maven i Node.js 22:

```sh
make dev-backend
make dev-frontend
```

Consulta el [full de ruta](docs/ROADMAP.md) i les [regles del projecte](AGENTS.md).
