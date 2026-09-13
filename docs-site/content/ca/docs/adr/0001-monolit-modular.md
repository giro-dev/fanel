---
title: "Monòlit modular amb Spring Boot + Spring Modulith"
linkTitle: "0001-monolit-modular"
weight: 10
description: Generat automàticament des de docs/adr/0001-monolit-modular.md. No editis aquest fitxer directament.
---

**Estat**: Acceptat · **Data**: 2026-09-04

## Context
Fanel és una app familiar (<10 usuaris per llar) auto-allotjable. El requisit principal d'operació és la simplicitat: un sol contenidor. Alhora ha de ser extensible (nous mòduls: receptes, automatitzacions, agents) sense degradar-se en un "big ball of mud".

## Opcions
| Opció | Pros | Contres |
|---|---|---|
| Microserveis | Escalat independent | Complexitat operativa enorme per a una família; múltiples contenidors |
| Monòlit clàssic per capes | Senzill | Acoblament creixent, difícil d'extreure res després |
| **Monòlit modular (Spring Modulith)** | Un desplegable; fronteres verificades per tests; esdeveniments persistits; documentació generada | Disciplina de paquets; dependència d'un projecte Spring addicional |

## Decisió
Monòlit modular amb Spring Boot 4.x, Java 25 i Spring Modulith 2.x. Un paquet Java per mòdul de negoci (`household`, `menu`, `recipes`, `shopping`, `calendar`, `chores`, `assistant`, `notifications`, `automation`) més `shared` (OPEN). Cada mòdul exposa `api/` (interfície, DTOs, esdeveniments) i amaga `domain/`, `infra/`, `web/`. La comunicació entre mòduls es fa per esdeveniments de domini amb el registre de publicació persistit (JPA). `ApplicationModules.verify()` s'executa al CI.

## Conseqüències
- Es pot extreure un mòdul a un servei propi si mai calgués, però no és l'objectiu.
- Cap mòdul importa `domain`/`infra` d'un altre; si es necessita, es reconsidera el disseny o s'amplia l'`api`.
- Els esdeveniments són també la base del canal SSE i dels agents reactius (`automation`).
