---
title: "Cap llibreria de components UI general; `FullCalendar` (nucli MIT) només per al calendari"
linkTitle: "0008-llibreria-ui-calendari"
weight: 80
description: Generat automàticament des de docs/adr/0008-llibreria-ui-calendari.md. No editis aquest fitxer directament.
---

**Estat**: Acceptat · **Data**: 2026-09-06

## Context

El `Calendar.tsx` actual mostra els esdeveniments com una llista agrupada per dia (sense vista de graella mensual/setmanal). Es va demanar avaluar `Untitled UI React` (https://www.untitledui.com/react/components/calendars) per al component de calendari, i alhora avaluar-ne l'adopció com a llibreria de components **general** per a tota la SPA. També es va revisar un recull d'alternatives ("9 React Calendar Components for Your Next App", dev.to/jaydevm) que inclou `react-calendar`, `react-big-calendar`, `react-datepicker`, `mantine`, `PrimeReact`, `MUI X` i `FullCalendar`.

## Opcions avaluades

### Llibreria general de components

| Opció | Pros | Contres |
|---|---|---|
| **Untitled UI React** | Catàleg enorme (100+ components), qualitat visual alta, React Aria per sota (accessibilitat) | **De pagament**: els components d'"Application UI" (inclòs `Calendars`) requereixen llicència PRO SOLO des de $349/usuari; construït sobre **Tailwind CSS**, que aquest projecte no usa (ADR 0002/0007 assumeixen CSS pla); adoptar-la implicaria introduir tota la cadena de build de Tailwind i re-escriure `styles.css` només per a una app d'ús familiar amb ~6 pantalles |
| **shadcn/ui** | Gratuït, MIT, es copia el codi (no és una dependència de node_modules) | També sobre Tailwind + Radix; mateix problema d'introduir Tailwind per poc benefici en aquesta mida de projecte |
| **react-aria-components** (Adobe, Apache-2.0, gratuït) | Sense estils (s'adapta al nostre CSS pla), i18n natiu (30+ idiomes, inclòs formatatge de dates/números localitzat), accessibilitat de primer nivell, sense dependència de Tailwind | És una capa de primitives (calendaris de selecció de data, comboboxes, etc.), no un calendari d'esdeveniments/agenda — no resol directament la necessitat actual |
| **Cap llibreria (statu quo)** | Zero dependències noves, control total, CSS pla ja coherent amb ADR 0002 | Cal construir a mà qualsevol component complex (com ara ho fem) |

### Component de calendari (esdeveniments)

| Opció | Pros | Contres |
|---|---|---|
| **Untitled UI Calendars** | Disseny polit | De pagament; sobre Tailwind; el "calendar" d'Untitled UI és un *date picker* de selecció de dia, no una agenda d'esdeveniments amb vistes mes/setmana |
| **react-big-calendar** (MIT, gratuït) | Agenda d'esdeveniments real amb vistes mes/setmana/dia, madur (des de 2015), es correspon amb el nostre domini (`CalendarEvent` amb `date`+`time` opcional) | Suport tàctil/mòbil més fluix (rellevant pel requisit de "mode tauleta" del ROADMAP); manteniment més irregular que FullCalendar |
| **react-calendar**, **react-datepicker**, **react-day-picker** | Lleugers, gratuïts | Són *date pickers* de selecció de dia, no agendes d'esdeveniments — no resolen la necessitat |
| **mantine** / **PrimeReact** / **MUI X** | Molt complets | Són suites de components senceres (com Untitled UI): adoptar-les només pel calendari introduiria un sistema de disseny complet que no volem (mateix problema que amb Untitled UI) |
| **FullCalendar** (`@fullcalendar/react` + `daygrid`/`timegrid`/`list`/`interaction`, MIT) | Nucli 100% MIT i gratuït (només els plugins de recursos/timeline són de pagament, i no els necessitem); vistes mes/setmana/dia/llista; millor suport tàctil i mòbil que `react-big-calendar` (rellevant per al "mode tauleta"); embolcall React oficial i molt mantingut; sense dependència de Tailwind | Diverses dependències (`core`+`react`+`daygrid`+`timegrid`+`list`+`interaction`); CSS propi que cal ajustar una mica a la nostra paleta |
| **Statu quo (llista agrupada per dia)** | Ja fet, zero dependències | No dona vista de mes/setmana, pitjor per planificar recurrències (Fase 2) |

## Decisió

1. **No s'adopta cap llibreria de components general** (ni Untitled UI, ni shadcn/ui, ni mantine/PrimeReact/MUI X, ni cap altra que impliqui adoptar un sistema de disseny sencer o Tailwind). El cost (llicència de pagament a Untitled UI, o una cadena de build nova en qualsevol cas) és desproporcionat per a una app auto-allotjada d'ús familiar amb poques pantalles; el CSS pla actual (ADR 0002/0007) ja compleix.
2. **S'adopta el nucli MIT de `FullCalendar` (`@fullcalendar/core`, `@fullcalendar/react`, `@fullcalendar/daygrid`, `@fullcalendar/timegrid`, `@fullcalendar/list`, `@fullcalendar/interaction`, versió `6.1.x`) només per al mòdul `calendar`**, substituint la llista agrupada per dia per vistes reals de mes/setmana/llista. Es fixa la sèrie `6.1.x` (no `7.x`) perquè la v7 és una reescriptura molt recent (primera estable el 2026-06-19, basada en un polyfill de `Temporal`) amb un canvi d'API de plugins; la 6.1.x és la sèrie establerta i àmpliament documentada des de fa anys.
3. Es descarta `react-aria-components` **per ara**: és una bona opció si en el futur calen widgets complexos (selector de dates al formulari de calendari/menú, combobox de receptes, etc.) que necessitin accessibilitat de teclat robusta, però no aporta res que el `<input type="date">` natiu no doni avui. Es pot reconsiderar puntualment, component a component, sense que sigui una decisió de "llibreria general".

## Conseqüències

- Noves dependències de frontend: `@fullcalendar/core`, `@fullcalendar/react`, `@fullcalendar/daygrid`, `@fullcalendar/timegrid`, `@fullcalendar/list`, `@fullcalendar/interaction` (totes MIT, sèrie 6.1.x, sense cost, sense Tailwind).
- El CSS de FullCalendar s'ajusta amb variables CSS pròpies a `styles.css` per mantenir la paleta de l'app.
- Cap canvi a la resta de pantalles (`Menu`, `Shopping`, `Chores`, `Recipes`, `Households`) ni a la cadena de build.
- No s'utilitzen els plugins Premium (`scrollgrid`, `timeline`, `resource-*`); si mai calguessin (p. ex. una vista de recursos per membre), caldria revisar aquest ADR i la llicència comercial corresponent.
- El bundle de producció creix d'uns 107 KB a ~186 KB (gzip); acceptable per a aquesta app, però si mai calgués prim-lo més, es pot carregar `Calendar.tsx` amb `import()` dinàmic (code-splitting) ja que és l'única pantalla que necessita FullCalendar.
- Si en el futur calgués un catàleg de components més ampli (p. ex. per a un disseny molt més elaborat), caldria un ADR nou que substitueixi aquest, avaluant el cost real (llicència + Tailwind) contra el valor concret que aportaria.
