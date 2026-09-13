---
title: "Spring AI amb proveïdor configurable; MCP server integrat"
linkTitle: "0006-spring-ai-mcp"
weight: 60
description: Generat automàticament des de docs/adr/0006-spring-ai-mcp.md. No editis aquest fitxer directament.
---

**Estat**: Acceptat · **Data**: 2026-09-04

## Context
Es vol un assistent d'IA dins l'app i que agents externs (Claude Desktop/Code, Home Assistant, agents propis) puguin operar sobre les dades via MCP. Sense obligar a cap proveïdor ni a cap cost.

## Opcions
| Opció | Pros | Contres |
|---|---|---|
| Servei d'agents separat (Python/LangChain) | Ecosistema IA ampli | Segon contenidor; duplicació de la lògica de domini |
| **Spring AI al mateix procés** | Tools = mètodes Java ja provats; MCP server/client inclosos; proveïdor per propietats; Ollama local | API encara evolutiva (2.x) |

## Decisió
Spring AI 2.x, confinat al mòdul `assistant`. Tres capes sobre les mateixes `@Tool`:
1. Tools de domini que criden les `api/` dels mòduls.
2. Assistent intern (`ChatClient`, memòria JDBC).
3. Servidor MCP a `/mcp` (Streamable HTTP, `spring-ai-starter-mcp-server-webmvc`) amb tools, resources i prompts; autenticació per token de household amb àmbits.

**Cap proveïdor actiu per defecte**: si no hi ha configuració `spring.ai.*`, el mòdul `assistant` no s'activa i l'app funciona igualment. Ollama, OpenAI i Anthropic són intercanviables per configuració.

## Conseqüències
- Qualsevol canvi d'API de Spring AI afecta només `assistant`.
- Les tools no contenen lògica de negoci: deleguen a les APIs de mòdul.
- Més endavant, MCP *client* per consumir serveis externs des de l'assistent.
