---
title: Referència API
linkTitle: Referència API
weight: 20
---

Fanel exposa un API REST `/api/v1` documentat amb OpenAPI (vegeu
[ADR 0004](../adr/0004-api-first-clients/)). L'especificació que veus aquí
sota es genera automàticament a cada build de la documentació a partir del
backend real, així que reflecteix sempre l'última versió a `main`.

Si prefereixes consultar-la directament: [`openapi.json`](/openapi.json).

<div id="api-reference"></div>

<script src="https://cdn.jsdelivr.net/npm/@stoplight/elements/web-components.min.js"></script>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@stoplight/elements/styles.min.css">

<elements-api
  id="fanel-api"
  apiDescriptionUrl="/openapi.json"
  router="hash"
  layout="sidebar"
></elements-api>
