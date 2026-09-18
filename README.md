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

## Configuració de la intel·ligència artificial

Per defecte cap proveïdor d'IA està actiu. Pots activar-ne tants com vulguis i
assignar-ne un a cada agent del xat, de manera que funcions com el reconeixement
d'imatges puguin anar a un model potent mentre el xat general funciona amb un
model local.

### Proveïdors suportats

| Proveïdor | Variable obligatòria | Exemple de model |
|---|---|---|
| Ollama | `SPRING_AI_OLLAMA_BASE_URL` | `llama3.2` |
| OpenAI | `SPRING_AI_OPENAI_API_KEY` | `gpt-4o` |
| Anthropic | `SPRING_AI_ANTHROPIC_API_KEY` | `claude-3-5-sonnet-20241022` |

La URL d'Ollama ja té un valor per defecte (`http://localhost:11434`), però
pots sobreescriure-la amb la variable. OpenAI i Anthropic només es configuren
quan la clau corresponent està present.

### Agents

Els agents es defineixen al fitxer `backend/src/main/resources/application.yml`
sota el prefix `fanel.assistant.agents`. Cada agent pot escollir proveïdor, model,
temperature, prompt i si accepta imatges.

```yaml
spring:
  ai:
    ollama:
      base-url: ${SPRING_AI_OLLAMA_BASE_URL:http://localhost:11434}
    openai:
      api-key: ${SPRING_AI_OPENAI_API_KEY:}
    anthropic:
      api-key: ${SPRING_AI_ANTHROPIC_API_KEY:}

fanel:
  assistant:
    agents:
      general:
        name-key: assistant.general.name
        description-key: assistant.general.description
        prompt-key: general
        supports-media: false
        tools:
          - AssistantTools
        model:
          provider: ollama
          model: llama3.2
          temperature: 0.7
      recipe-from-image:
        name-key: assistant.recipe-from-image.name
        description-key: assistant.recipe-from-image.description
        prompt-key: recipe-from-image
        supports-media: true
        tools:
          - AssistantTools
        model:
          provider: openai
          model: gpt-4o-mini
          temperature: 0.2
```

- `name-key` / `description-key`: claus de traducció del frontend (`ca.json` / `es.json`).
- `prompt-key`: nom del fitxer de `backend/src/main/resources/assistant/prompts/`.
  S'afegeix automàticament l'idioma (`_ca.txt` / `_es.txt`).
- `supports-media`: permet enviar imatges en base64 a aquest agent (cal un model
  amb visió).
- `tools`: llista de beans de tools disponibles. `AssistantTools` és l'únic
  disponible actualment; permet a l'agent consultar i modificar dades de la llar.
  Deixa-ho buit (`[]`) si no vols que faci tool-calling.

Des del frontend apareixerà un botó de xat flotant; només es mostraran els agents
que hagis configurat i per als quals el proveïdor estigui disponible.
