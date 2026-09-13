---
title: "Pla original"
linkTitle: "PLA-ORIGINAL"
weight: 60
description: Generat automàticament des de docs/PLA-ORIGINAL.md. No editis aquest fitxer directament.
---

> Document de planificació prèvia al codi que va originar aquest repositori. Es conserva
> íntegre com a referència de context i de "per què" de les decisions. Les decisions
> d'arquitectura vinculants viuen a [`AGENTS.md`](https://github.com/giro-dev/fanel/blob/main/AGENTS.md) i als [ADRs](../../adr/); l'estat
> real d'implementació viu a [`ROADMAP.md`](../). Si aquest document i el `ROADMAP.md`
> discrepen, **el `ROADMAP.md` mana** (és el que es manté al dia).
>
> Decisions ja preses respecte a la secció 6 d'aquest document (veure `AGENTS.md`/ADRs):
> 1. Frontend: **React + TypeScript** (Vite, PWA).
> 2. BD: **PostgreSQL i SQLite** suportats des del dia 1.
> 3. Nom/paquet: **Fanel** (*the family organizer panel*), paquet `dev.agiro.fanel`. UI per
>    defecte en **català**, amb castellà com a segon idioma.
> 4. **Multi-household** des del model inicial.
> 5. Proveïdor d'IA per defecte: **cap actiu per defecte**, configurable (Ollama/OpenAI/Anthropic).

---

## 1. Què fa la POC (i què n'aprenem)

| Mòdul | POC | Què cal per a la versió real |
|---|---|---|
| Menú | Graella dia × (esmorzar, dinar, berenar, sopar), text lliure | Setmanes reals (dates, no "Dilluns" genèric), receptes reutilitzables, generació de compra a partir del menú |
| Calendari | Llista d'esdeveniments amb data + títol + qui l'ha afegit | Hora, recurrència, per membre, sincronització CalDAV/ICS |
| Compra | Ítems amb `done`, "buida comprats" | Quantitats, categories/passadissos, múltiples llistes, mode botiga offline |
| Tasques | Text + assignat (Mamà/Papà/Nens) + `done` | Membres reals, recurrència, rotació, gamificació opcional |
| Identitat | `prompt('¿Cómo te llamas?')` local | Llar (household) + membres + login real però *senzill* (família, no empresa) |
| Persistència | `window.storage` clau-valor JSON | BD relacional, esdeveniments de domini, API estable |

Conclusions: el model de dades és petit però **totes les funcions es creuen** (menú → compra, tasques → calendari, receptes → compra). L'arquitectura ha de facilitar aquestes interaccions sense acoblar els mòduls.

---

## 2. Decisions d'arquitectura

### 2.1 Forma general: **monòlit modular en un sol contenidor**

```
┌──────────────────── Docker (1 contenidor) ────────────────────┐
│  Spring Boot 4.x (Java 25)                                     │
│  ├─ /            → SPA estàtica (build del frontend dins el JAR)│
│  ├─ /api/v1/**   → REST (OpenAPI 3.1 generat)                  │
│  ├─ /api/v1/events → SSE (temps real: canvis a llistes/menú)   │
│  ├─ /mcp         → MCP Server (Streamable HTTP) — Spring AI    │
│  ├─ /ai/**       → Assistent (Spring AI ChatClient + tools)    │
│  └─ /actuator    → health, metrics                             │
│  Persistència: PostgreSQL (extern o SQLite embegut, veure 2.3) │
└────────────────────────────────────────────────────────────────┘
```

**Per què no microserveis / 2 contenidors**: una família té <10 usuaris; la complexitat operativa és el principal enemic de l'auto-allotjament. El monòlit modular (Spring Modulith) dóna les fronteres internes que permeten extreure un mòdul més endavant si mai calgués.

### 2.2 Backend: Spring Boot + Spring Modulith + Spring AI

Versions estables a data d'avui (verificar a l'inici): Spring Boot 4.1.x, Spring AI 2.0.x, Spring Modulith 2.1.x, Java 25 LTS.

**Spring Modulith** (recomanació clau per l'extensibilitat):
- Un paquet Java per mòdul de negoci: `household`, `menu`, `recipes`, `shopping`, `calendar`, `chores`, `assistant`, `notifications`.
- Cada mòdul exposa una API pública (interfícies + DTOs) i amaga la implementació. Modulith **verifica les dependències als tests** (`ApplicationModules.verify()`), així no es degrada amb el temps.
- Comunicació entre mòduls via **esdeveniments de domini** (`@ApplicationModuleListener`, event publication registry persistit) → `MenuPlanned` → `shopping` afegeix ingredients; `ChoreCompleted` → `notifications`. Els mateixos esdeveniments alimenten el canal SSE i, en el futur, agents.
- Genera documentació de mòduls (C4/PlantUML) automàticament.

Pros: extensible per disseny, un sol desplegable, test per mòdul. Contres: disciplina de paquets; corba d'aprenentatge petita.

**Capes dins de cada mòdul** (senzill, no DDD tàctic complet):
`api/` (interfície pública + DTO + events) · `web/` (controller) · `domain/` (entitats + serveis) · `infra/` (repositoris JPA).

**Persistència**: Spring Data JPA + **Flyway** (migracions versionades des del dia 1). IDs UUIDv7 (ordenables). Auditoria (`createdBy`, `createdAt`) via `@EntityListeners` — substitueix l'`addedBy` de la POC.

**API**: REST JSON `/api/v1` + `springdoc-openapi` → l'OpenAPI és el **contracte per als clients Android/escriptori** (generació de clients Kotlin/TS). Errors amb RFC 9457 Problem Details (Spring ho suporta nativament).

**Temps real**: SSE (`SseEmitter`) per a "algú ha marcat la llet com comprada". Més senzill que WebSocket, funciona rere qualsevol proxy, i Android ho consumeix bé (OkHttp). WebSocket només si calgués bidireccional.

### 2.3 Base de dades: PostgreSQL per defecte, SQLite com a mode "zero-config"

| Opció | Pros | Contres |
|---|---|---|
| **PostgreSQL** (compose amb 2 serveis: app + db) | Estàndard, pgvector per RAG, backups coneguts | Trenca "1 docker" estrictament (però és el patró habitual d'auto-allotjament: app + db) |
| **SQLite** embegut (fitxer al volum) | Realment 1 contenidor, backup = copiar fitxer, suficient per a una família | JDBC menys madur amb JPA (dialecte Hibernate community), sense pgvector (vector store alternatiu: SimpleVectorStore o no RAG) |
| **H2 fitxer** | Fàcil amb Spring | No recomanat en producció |

**Recomanació**: dissenyar l'app perquè funcioni amb totes dues via perfils (`sqlite` per defecte per a "descarrega i executa", `postgres` recomanat). Flyway amb SQL portable (o dos directoris de migracions si cal). Això dóna la millor experiència d'auto-allotjament sense tancar portes. Si vols reduir complexitat inicial: comença **només amb PostgreSQL** i afegeix SQLite a la fase 3.

### 2.4 Frontend: SPA estàtica servida pel JAR

Requisit: un sol artefacte, sense Node en producció. El build del frontend es copia a `src/main/resources/static` (plugin `frontend-maven-plugin` o mòdul Maven `ui` que empaqueta el `dist/` com a recurs). Spring serveix `index.html` per a rutes desconegudes (SPA fallback).

Alternatives comparades:

| Opció | Pros | Contres | Encaix amb Android/escriptori |
|---|---|---|---|
| **React + Vite + TS** (o Preact) | Ecosistema més gran, components per a tot, TanStack Query, PWA fàcil | Bundle mitjà; molt boilerplate | Capacitor/TWA per Android; Tauri/Electron escriptori; codi de domini TS compartible |
| **Vue 3 + Vite + TS** | Corba suau, SFC llegibles, Pinia, bon per a un dev sol | Ecosistema menor que React | Igual que React |
| **Svelte 5 / SvelteKit (adapter-static)** | Bundle mínim, molt ràpid en mòbil, poc codi | Ecosistema més petit; runes són noves | Igual |
| **Angular** | Tot inclòs, tipat fort, coherent amb mentalitat Java | Pesat per a un projecte familiar | Igual |
| **Thymeleaf + htmx** | Zero build JS, tot a Java | Difícil de reutilitzar per a Android; UX offline pobra | Dolent |
| **Flutter Web** | Un codi per web+Android+escriptori | Web pesat (CanvasKit), SEO/accessibilitat pitjor, sortim de l'ecosistema web | Excel·lent (però vegeu 2.6) |
| **Kotlin Multiplatform + Compose Multiplatform** | Kotlin comparteix mentalitat amb Java; Android natiu de primera | Web encara experimental (Wasm) | Excel·lent en Android, web immadur |

**Recomanació**: **Vue 3 o React amb Vite + TypeScript, com a PWA** (manifest + service worker via `vite-plugin-pwa`).
- PWA cobreix el 80% del cas Android d'una família (icona a l'escriptori, offline per a la llista de la compra a la botiga, notificacions push web).
- El mateix build es pot embolcallar amb **Capacitor** (Android nadiu amb widgets/notificacions natives) i **Tauri** (escriptori) sense reescriure.
- El client OpenAPI generat (`openapi-typescript`) es comparteix entre web, Capacitor i Tauri.
- Si prefereixes menys "framework": Svelte és perfectament vàlid i més lleuger; el risc és només d'ecosistema.

Entre Vue i React: Vue si prioritzes rapidesa de desenvolupament en solitari i llegibilitat; React si vols màxim ecosistema (shadcn/ui, Radix) i més ajuda dels LLM/agents de codi. Els dos són bones eleccions; **jo triaria React + TS** per la densitat de components de qualitat i perquè els agents de codi generen React amb menys errors.

### 2.5 IA, agents i MCP (Spring AI)

Tres capes, totes al mateix procés:

1. **Tools de domini (`@Tool`)**: `addShoppingItem`, `planMeal`, `listUpcomingEvents`, `assignChore`… Són mètodes Java anotats que criden l'API pública dels mòduls. **Es defineixen una sola vegada** i es reutilitzen per (2) i (3).
2. **Assistent intern**: `ChatClient` de Spring AI amb les tools + memòria de conversa (JDBC `ChatMemoryRepository`). Proveïdor intercanviable via config: OpenAI, Anthropic, **Ollama** (local, coherent amb auto-allotjament) — Spring AI abstrau tots. Casos: "planifica la setmana amb el que hi ha a la nevera", "afegeix el que falta pel sopar de dijous a la compra", "qui té tasques pendents?".
3. **MCP Server** (`spring-ai-starter-mcp-server-webmvc`, protocol `STREAMABLE`): exposa les mateixes tools (+ resources: menú de la setmana, llista actual; + prompts) a agents externs — Claude Desktop, Claude Code, agents casolans, Home Assistant. Protegit amb token/API key per household.

També **MCP Client** més endavant: l'assistent pot consumir MCPs externs (calendari Google, supermercat, Home Assistant).

Extensibilitat d'agents: els esdeveniments de domini (Modulith) permeten afegir "agents reactius" (p. ex. cada diumenge `WeekStarted` → l'agent proposa un menú i el deixa en esborrany). Un mòdul `automation` amb regles/programacions (`@Scheduled` + Spring Modulith events) és el lloc natural.

Pros de Spring AI: mateixa JVM, tools = mètodes Java ja provats, MCP servidor i client out-of-the-box, canvi de proveïdor per propietats. Contres: API encara evoluciona (2.0 ha trencat coses respecte 1.x); RAG/vector store només si cal (probablement no al principi).

### 2.6 Clients Android i escriptori (visió)

Ordre recomanat, de menor a major esforç:
1. **PWA** instal·lable (fase 1) → ja usable a Android i escriptori.
2. **Capacitor** (fase 4) → APK real, notificacions push natives, widget de llista de la compra, compartir-a-l'app. Reutilitza el 100% de la SPA.
3. **Tauri** (fase 4/5) → app d'escriptori lleugera, mateix codi.
4. **App Kotlin nativa** només si es vol widget/Wear OS avançat; aleshores el client OpenAPI generat en Kotlin i SSE/MCP ja hi són. Mantenir la lògica de negoci al servidor fa que aquest camí sigui viable sense duplicar regles.

Requisits que ho fan possible i que cal complir des de la fase 1: **API first (OpenAPI), autenticació per token (no només cookie), CORS configurable, zero lògica de negoci al frontend.**

### 2.7 Seguretat i identitat (senzilla, familiar)

- Model: `Household` → `Member` (rol `ADULT`/`CHILD`, avatar, color). Un servidor pot allotjar 1..n households (per a amics/família extensa) — decidir-ho ara evita una migració dolorosa.
- Login: Spring Security amb usuari/contrasenya + **JWT o sessió + token d'API** per a clients mòbils i MCP. Opcional: OIDC (Authelia/Authentik/Google) via `spring-boot-starter-oauth2-client` per als que ja tenen SSO a casa.
- "Mode tauleta de cuina": dispositiu compartit amb PIN per canviar de membre (hereta el `¿Quién eres?` de la POC).
- Tokens d'API per a agents/MCP amb àmbits (lectura/escriptura per mòdul).

### 2.8 Operacions / auto-allotjament

- `Dockerfile` multi-stage (build frontend → build Maven → imatge runtime `eclipse-temurin:25-jre` o distroless). Imatge multi-arch (amd64 + **arm64** per a Raspberry Pi / NAS).
- `docker-compose.yml` d'exemple (app + postgres) i variant SQLite d'un sol servei.
- Configuració 100% per variables d'entorn; `/actuator/health` per a healthchecks.
- Backups: script/endpoint d'export (JSON/ZIP) + import; en SQLite, copiar el fitxer.
- Imatge publicada a GHCR via GitHub Actions; release amb tags semver.
- Observabilitat mínima: logs estructurats, Micrometer; Modulith Insight opcional.

### 2.9 Estructura del repositori (monorepo)

```
family-hub/                     (nom provisional — finalment "Fanel", dev.agiro.fanel)
├─ backend/                     Maven multi-module o mòdul únic amb paquets Modulith
│  └─ src/main/java/dev/agiro/familyhub/
│     ├─ household/  menu/  recipes/  shopping/  calendar/  chores/
│     ├─ assistant/  (Spring AI: tools, ChatClient, MCP server)
│     ├─ notifications/  automation/
│     └─ shared/  (events base, auditing, errors)
├─ frontend/                    Vite + TS (PWA); build → backend/src/main/resources/static
├─ clients/                     (futur) android-capacitor/, desktop-tauri/
├─ deploy/                      Dockerfile, compose, exemples .env
├─ docs/                        Hugo (coherent amb matriarch), ADRs
└─ .github/workflows/           CI (test + verify Modulith), build imatge, docs
```

Convencions coherents amb els teus repos: Java 21+/Maven, Hugo per a docs, ADRs (`docs/adr/0001-monolit-modular.md`…) per registrar aquestes decisions.

---

## 3. Model de domini inicial

```
Household(id, name, locale, timezone)
Member(id, householdId, name, role, color, pin?, credentials?)
ApiToken(id, memberId|householdId, scopes[], hash, expiresAt)

Recipe(id, householdId, name, servings, ingredients[] {name, qty, unit, category}, tags[], notes)
MealPlan(id, householdId, weekStart)                   -- setmana ISO
  MealSlot(planId, date, mealType[BREAKFAST|LUNCH|SNACK|DINNER], recipeId?, freeText?, forMembers[]?)

ShoppingList(id, householdId, name, isDefault)
  ShoppingItem(id, listId, name, qty?, unit?, category?, done, addedBy, source[MANUAL|MEALPLAN|RECURRING])

CalendarEvent(id, householdId, title, start, end?, allDay, rrule?, memberIds[], source[LOCAL|ICS])
Chore(id, householdId, title, assigneeId?, dueDate?, rrule?, rotation[]?, done, completedBy, completedAt)

Events de domini: MealPlanned, ShoppingItemAdded/Checked, ChoreCompleted, EventCreated…
```

> Nota d'implementació (2026-09): el model real difereix lleugerament d'aquest esborrany
> inicial (p. ex. `MealSlot` amb text lliure en lloc de `recipeId` fins que `recipes` existeixi,
> `CalendarEvent` sense `rrule` encara). Vegeu el codi a `backend/src/main/java/dev/agiro/fanel/`
> i el `ROADMAP.md` per l'estat real mòdul a mòdul.

---

## 4. Roadmap (per fases, cada fase és desplegable)

Estimació orientativa en sessions de treball (Devin) — les esperes externes (Play Store, dominis) van a part.

### Fase 0 — Fonaments (1 sessió)
- Repo, Maven, Spring Boot 4 + Modulith + JPA + Flyway + Security bàsica, perfils `postgres`/`sqlite`(opcional).
- Frontend Vite+TS+PWA esquelet, integrat al build (un sol JAR).
- Dockerfile multi-stage + compose + CI (tests, `ApplicationModules.verify()`, imatge a GHCR).
- ADRs 0001–0005 (monòlit modular, SPA embeguda, BD, auth, IA/MCP).
- **Done**: `docker compose up` → login → pàgina buida servida des del JAR.

### Fase 1 — Paritat amb la POC (1–2 sessions)
- Mòduls `household`, `menu` (setmana real), `shopping`, `calendar`, `chores` amb REST + OpenAPI.
- UI de les 4 pestanyes + selector de membre ("qui sóc") + mode tauleta.
- SSE per a actualitzacions en temps real entre dispositius.
- Export/import JSON.
- **Done**: la família el fa servir cada dia en substitució de la POC.

### Fase 2 — Valor afegit de domini (1–2 sessions)
- `recipes` + generar llista de la compra des del menú (via esdeveniments).
- Categories/passadissos, múltiples llistes, ítems recurrents.
- Recurrència (RRULE) a calendari i tasques; rotació de tasques.
- Notificacions web push (recordatoris de tasques/esdeveniments).
- Offline real per a la llista de la compra (cache + cua de sincronització al service worker).

### Fase 3 — IA i MCP (1–2 sessions)
- `assistant`: tools `@Tool` sobre tots els mòduls, `ChatClient` amb memòria JDBC, proveïdor configurable (Ollama per defecte local, OpenAI/Anthropic opcional).
- Xat a la UI ("planifica'm la setmana", "què falta per al sopar?").
- **MCP Server** Streamable HTTP a `/mcp` amb tokens per household; provat amb Claude Desktop/Code.
- Mòdul `automation`: regles programades (proposta de menú dominical, recordatori de compra divendres).
- Importació d'ICS/CalDAV (només lectura primer).

### Fase 4 — Clients (1–2 sessions + esperes externes)
- Capacitor → APK Android (push natiu, compartir text a la llista, widget senzill).
- Tauri → escriptori (Linux/Windows/macOS).
- Client OpenAPI generat (TS ara; Kotlin si es fa app nativa).

### Fase 5 — Maduresa (contínua)
- MCP client (Home Assistant, calendari extern), RAG sobre receptes (pgvector) si aporta valor.
- Multi-household, OIDC, internacionalització (ca/es/en), accessibilitat, mètriques.

---

## 5. Riscos i mitigacions

| Risc | Mitigació |
|---|---|
| Spring AI 2.x canvia API | Aïllar tot Spring AI al mòdul `assistant`; les tools criden APIs de mòdul estables |
| Sobre-enginyeria per a una app familiar | Modulith sense DDD tàctic complet; fase 1 estrictament paritat POC |
| SQLite + Hibernate | Començar només Postgres si dóna problemes; SQLite com a millora posterior |
| Frontend dins el JAR alenteix el build | Mòdul Maven `ui` separat + cache de node al CI; en dev, Vite amb proxy a `:8080` |
| Auth massa complexa per a nens/tauleta | Mode tauleta amb PIN; contrasenyes només per adults |

---

## 6. Decisions preses (originalment demanades abans de la Fase 0)

1. **Frontend**: ✅ React + TS (Vite, PWA). Veure ADR 0002.
2. **BD**: ✅ Postgres + SQLite des del dia 1. Veure ADR 0003.
3. **Nom del projecte / package**: ✅ **Fanel**, `dev.agiro.fanel`. UI per defecte: **català**.
4. **Multi-household**: ✅ sí, des del model inicial. Veure ADR 0005.
5. **Proveïdor d'IA per defecte**: ✅ cap actiu per defecte, configurable. Veure ADR 0006.
