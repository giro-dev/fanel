---
title: Fanel
---

{{% blocks/cover title="Fanel — el panell d'organització familiar" image_anchor="top" height="med" %}}
<a class="btn btn-lg btn-primary me-3 mb-4" href="/docs/">
  Documentació <i class="fa-solid fa-circle-right ms-2"></i>
</a>
<a class="btn btn-lg btn-secondary me-3 mb-4" href="https://github.com/giro-dev/fanel">
  Codi font <i class="fa-brands fa-github ms-2"></i>
</a>
<p class="lead mt-5">Menú setmanal, receptes, llista de la compra, calendari i tasques per a la llar, amb assistent d'IA opcional.</p>
{{% /blocks/cover %}}

{{% blocks/lead %}}
Fanel és una aplicació **auto-allotjable**: un únic contenidor Docker amb el backend
(Spring Boot) i el client web (React) empaquetats junts. També hi ha un client
Android nadiu.
{{% /blocks/lead %}}

{{% blocks/section %}}
{{% blocks/feature icon="fa-brands fa-docker" title="Un sol contenidor" %}}
Backend, frontend i base de dades (PostgreSQL o SQLite) desplegats amb
`docker compose up`. Sense microserveis.
{{% /blocks/feature %}}

{{% blocks/feature icon="fa-solid fa-mobile-screen" title="Client Android" %}}
App nadiu en Jetpack Compose amb suport offline, sincronitzada amb el mateix API.
{{% /blocks/feature %}}

{{% blocks/feature icon="fa-solid fa-robot" title="Assistent d'IA opcional" %}}
Configura Ollama, OpenAI o Anthropic per a un xat que gestiona el menú, la
compra i les tasques de la llar.
{{% /blocks/feature %}}
{{% /blocks/section %}}
