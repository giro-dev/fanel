---
title: "Accés remot transparent amb WireGuard"
linkTitle: "0012-acces-remot-wireguard"
weight: 120
description: Generat automàticament des de docs/adr/0012-acces-remot-wireguard.md. No editis aquest fitxer directament.
---

**Estat**: Acceptat · **Data**: 2026-09-23

## Context

Els clients mòbils necessiten accedir a la mateixa API auto-allotjada tant des de la LAN com des de fora, sense publicar l'API Fanel a Internet ni canviar el flux d'autenticació existent. El WireGuard del servidor ha de poder administrar peers de manera segura i l'usuari ha de poder vincular un dispositiu amb un QR d'un sol ús.

## Opcions avaluades

| Opció | Pros | Contres |
|---|---|---|
| **WireGuard al host de Fanel** | Topologia simple, cap servei nou; el túnel arriba directament a l'API | Requereix accés `NET_ADMIN` i configurar la interfície del host |
| WireGuard en un router o appliance separat | Separa xarxa i aplicació | Cal desplegar i mantenir un altre component; Fanel no pot administrar peers directament |
| Exposar l'API amb un reverse proxy públic | Fàcil per a clients | Augmenta superfície d'atac i no compleix l'accés privat desitjat |

## Decisió

S'adopta la primera opció, mantenint l'aplicació desactivada per defecte.

1. **Mòdul `wireguard`**: encapsula API, endpoints, domini i adaptador CLI com a mòdul Spring Modulith. El backend valida claus/IPs, parla amb `wg` mitjançant arguments estructurats i reconcilia periòdicament els peers de BD amb la interfície.
2. **Emparellament d'un sol ús**: l'admin global crea un token aleatori, emmagatzemat només com a hash; el QR codifica URLs LAN/túnel, l'identificador de servidor i l'empremta SPKI TLS. El token es consumeix atòmicament en l'enrolament.
3. **Peers i secrets**: les dades dels peers viuen a la BD comuna PostgreSQL/SQLite. Les PSK es xifren amb AES-256-GCM; no es registren tokens, PSK ni claus privades.
4. **Xarxa restringida**: la topologia A serveix HTTPS a `10.8.0.1:8443` i no dona accés general a la LAN. La configuració Docker usa la mateixa imatge, host networking i `NET_ADMIN`; la instal·lació JAR pot usar un helper root-owned amb sudoers restringit.
5. **Compatibilitat**: WireGuard és opt-in. Els endpoints es documenten sempre a OpenAPI, però els endpoints de funcionalitat responen 404 quan està desactivat; l'estat administratiu continua informant `enabled=false`. TLS també és opcional per a instal·lacions existents.
6. **Client**: el servidor i el panell web gestionen la vinculació i revocació. La implementació del túnel integrat al client Android és una fase separada; el flux d'autenticació Fanel no canvia.

## Conseqüències

- Cal protegir la clau de xifrat PSK, el password PKCS12 i la clau privada del servidor amb permisos i còpies de seguretat adequats.
- La topologia Docker requereix host networking, capacitat de xarxa i un certificat amb SAN que coincideixi amb les URLs anunciades.
- L'administrador és responsable de reenviar només UDP 51820 al router i aplicar regles de tallafoc restrictives.
- La reconciliació restaura peers actius després d'un reinici i pot retirar peers desconeguts només quan s'habilita explícitament.
- L'alternativa de túnel cap a la LAN queda documentada però no automatitzada.

## Referències

- [Guia d'instal·lació WireGuard](../wireguard.md)
- [Notes d'implementació](../wireguard-notes.md)
