---
title: "API first com a base per a clients"
linkTitle: "0004-api-first-clients"
weight: 40
description: Generat automàticament des de docs/adr/0004-api-first-clients.md. No editis aquest fitxer directament.
---

**Estat**: Acceptat · **Data**: 2026-09-04

## Context
Visió de futur: clients Android, escriptori i agents (MCP). Tots han de compartir les mateixes regles de negoci sense duplicar-les.

## Decisió
- Tota funcionalitat s'exposa primer com a REST JSON a `/api/v1/**`, documentada amb OpenAPI 3 (springdoc). El fitxer OpenAPI és el contracte: els clients es generen a partir d'ell (TS ara; Kotlin si cal).
- Errors amb RFC 9457 *Problem Details*.
- Temps real amb SSE (`/api/v1/events`), no WebSocket, tret que calgui bidireccionalitat.
- Autenticació utilitzable per clients no-navegador: token d'API (Bearer) a més de sessió; CORS configurable.
- Zero lògica de negoci al frontend.

## Conseqüències
- Un canvi incompatible d'API obliga a `/api/v2` o a versionar el recurs.
- El frontend web és "un client més" i pot ser substituït o duplicat (Capacitor, Tauri) sense tocar el servidor.
