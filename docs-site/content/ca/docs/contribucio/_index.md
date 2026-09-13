---
title: Guia de contribució
linkTitle: Contribució
weight: 30
---

Aquesta pàgina resumeix [`AGENTS.md`](https://github.com/giro-dev/fanel/blob/main/AGENTS.md),
la font de veritat sobre convencions del repositori (vinculant per a
persones i agents de codi).

## Arquitectura

Monòlit modular amb Spring Boot + Spring Modulith (un sol procés, un sol
JAR, un sol contenidor), frontend React/TypeScript compilat i servit des
del mateix JAR, PostgreSQL i SQLite suportats des del dia 1, API REST
`/api/v1` documentada amb OpenAPI, multi-household des de l'inici. Detalls
complets a les [ADRs]({{< relref "../adr" >}}).

## Estructura d'un mòdul de backend

```
<modul>/
  api/      interfície pública, DTOs (records) i esdeveniments  ← únic que altres mòduls poden importar
  web/      controllers REST (/api/v1/<recurs>)
  domain/   entitats JPA i serveis
  infra/    repositoris i adaptadors
  package-info.java  amb @ApplicationModule
```

Un mòdul **només** importa `*.api.*` d'altres mòduls i `shared`.
`ApplicationModules.verify()` ho comprova als tests: si falla, arregla el
disseny, no el test. Els efectes creuats entre mòduls es fan escoltant
esdeveniments (`@ApplicationModuleListener`), no cridant serveis d'un altre
mòdul directament.

## Convencions

- **Java 25**, Maven, Spring Boot 4.x. Records per a DTOs, `Optional` només
  com a retorn. Sense Lombok.
- **TypeScript strict**, components funcionals de React, TanStack Query,
  cap `any`.
- Text d'usuari sempre via i18n (`frontend/src/i18n/{ca,es}.json`,
  `backend/src/main/resources/messages*.properties`) — afegeix la clau a
  tots els idiomes.
- Idioma del codi i comentaris: anglès. Idioma de docs, commits i UI per
  defecte: català.
- Commits: [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`, `chore:`…).
- Cap secret al repositori; configuració només per variables d'entorn.

## Comandes

| Acció | Comanda |
|---|---|
| Build complet (frontend + backend, un JAR) | `mvn -B verify` |
| Backend sol (salta build del frontend) | `mvn -pl backend verify -Dskip.frontend` |
| Backend en dev (SQLite) | `make dev-backend` |
| Frontend en dev | `make dev-frontend` |
| Lint/typecheck frontend | `cd frontend && npm run lint && npm run typecheck` |
| Android debug + tests | `make android-build android-test` |
| Documentació en local | `make docs-serve` |

Abans d'obrir un PR: `mvn -B verify` verd (inclou `ApplicationModules.verify()`
i tests amb Postgres i SQLite) i lint/typecheck del frontend verds.

## Què no fer

- No afegir un segon servei/contenidor obligatori (excepte la BD Postgres
  opcional).
- No introduir Spring AI fora del mòdul `assistant`.
- No afegir lògica de negoci al frontend.
- No acoblar mòduls per `domain`/`infra` d'un altre mòdul.
- No canviar una decisió d'arquitectura sense un ADR nou.
