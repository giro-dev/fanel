# ADR 0003 — PostgreSQL i SQLite via perfils

**Estat**: Acceptat · **Data**: 2026-09-04

## Context
"Auto-allotjable" vol dir dues coses per a públics diferents: qui té un NAS/servidor amb Postgres vol el motor estàndard; qui vol "descarrega i executa" vol un sol contenidor i un backup que sigui copiar un fitxer.

## Opcions
| Opció | Pros | Contres |
|---|---|---|
| Només PostgreSQL | Estàndard, pgvector, tooling | Obliga a 2 contenidors |
| Només SQLite | 1 contenidor real, backup trivial | Sense pgvector; dialecte Hibernate community |
| H2 | Integració Spring fàcil | No apte per producció |
| **Postgres + SQLite per perfil** | Millor experiència per a tots dos públics | Cal SQL portable i tests dobles |

## Decisió
Perfils Spring `postgres` (per defecte) i `sqlite`. Flyway amb migracions a `db/migration/common` (SQL portable) i, només quan sigui imprescindible, `postgresql/` i `sqlite/` (via placeholder `{vendor}`). UUID emmagatzemats de manera portable. Els tests d'integració s'executen amb els dos motors a cada PR.

## Conseqüències
- Evitar funcionalitats específiques d'un motor a `common`; si es necessiten (pgvector a la Fase 5), es fan opcionals i condicionades al perfil.
- Tota nova migració es prova contra els dos motors abans de fusionar.
- Configuració per env: `FANEL_DB_URL/USER/PASSWORD` o `FANEL_SQLITE_PATH`.
