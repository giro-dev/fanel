# ADR 0002 — SPA React + TypeScript (PWA) servida des del JAR

**Estat**: Acceptat · **Data**: 2026-09-04

## Context
Cal una UI web moderna, usable des del mòbil (PWA instal·lable, offline a la botiga) i reutilitzable per a futurs clients Android i escriptori, sense necessitar Node ni un segon contenidor en producció.

## Opcions
| Opció | Pros | Contres |
|---|---|---|
| **React + Vite + TS** | Ecosistema màxim (components, TanStack Query), bona generació per agents de codi, Capacitor/Tauri | Bundle mitjà, més boilerplate |
| Vue 3 | Corba suau, molt productiu en solitari | Ecosistema menor |
| Svelte 5 | Bundle mínim | Ecosistema petit, runes recents |
| Angular | Tot inclòs | Pesat per a aquest abast |
| Thymeleaf + htmx | Zero build JS | No reutilitzable per a Android; offline pobre |
| Flutter / Compose Multiplatform | Un codi per a tot | Web pesat/immadur; fora de l'ecosistema web |

## Decisió
React 19 + TypeScript strict + Vite, com a PWA (`vite-plugin-pwa`). El build (`dist/`) s'empaqueta en el mòdul Maven `frontend` com a recurs `static/` i Spring Boot el serveix amb *fallback* a `index.html` per a rutes de la SPA. En desenvolupament, Vite fa proxy de `/api` al backend.

## Conseqüències
- Un sol artefacte (JAR) i una sola imatge Docker.
- Cap lògica de negoci al frontend: tot passa per l'API (vegeu ADR 0004).
- La mateixa SPA s'embolcallarà amb Capacitor (Android) i Tauri (escriptori) a la Fase 4.
- El build de Maven inclou `npm ci && npm run build` (`frontend-maven-plugin`); `-Dskip.frontend` per iterar només backend.
