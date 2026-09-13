---
title: Configuració de la intel·ligència artificial
linkTitle: Intel·ligència artificial
weight: 30
---

Per defecte cap proveïdor d'IA està actiu. Pots activar-ne tants com vulguis
i assignar-ne un a cada agent del xat, de manera que funcions com el
reconeixement d'imatges puguin anar a un model potent mentre el xat general
funciona amb un model local.

## Proveïdors suportats

| Proveïdor | Variable obligatòria | Exemple de model |
|---|---|---|
| Ollama | `SPRING_AI_OLLAMA_BASE_URL` | `llama3.2` |
| OpenAI | `SPRING_AI_OPENAI_API_KEY` | `gpt-4o` |
| Anthropic | `SPRING_AI_ANTHROPIC_API_KEY` | `claude-3-5-sonnet-20241022` |

La URL d'Ollama ja té un valor per defecte (`http://localhost:11434`), però
pots sobreescriure-la amb la variable. OpenAI i Anthropic només es
configuren quan la clau corresponent està present.

## Agents

Els agents es defineixen al fitxer `backend/src/main/resources/application.yml`
sota el prefix `fanel.assistant.agents`. Cada agent pot escollir proveïdor,
model, temperature, prompt i si accepta imatges:

```yaml
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
```

- `name-key` / `description-key`: claus de traducció del frontend.
- `prompt-key`: nom del fitxer de `backend/src/main/resources/assistant/prompts/`.
- `supports-media`: permet enviar imatges en base64 a aquest agent.
- `tools`: llista de beans de tools disponibles (`AssistantTools`).

Des del frontend apareixerà un botó de xat flotant; només es mostraran els
agents configurats i amb proveïdor disponible.
