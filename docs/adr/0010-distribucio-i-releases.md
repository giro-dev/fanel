# ADR 0010 — Distribució i estratègia de releases

**Estat**: Acceptat · **Data**: 2026-09-13

## Context

Fanel ja té un backend/frontend estable ([Fase 2](../ROADMAP.md) completa) i un
client Android natiu. Falta definir com es distribueixen els artefactes
(imatge Docker, APK) i com es publica documentació consultable sense clonar
el repositori. Fins ara: `docker.yml` publicava `ghcr.io/giro-dev/fanel:latest`
en cada push a `main` (i, per error, també amb tags `v*` sense versionar la
imatge); `android/` només es compilava i testava a CI, sense publicar cap
artefacte; no hi havia lloc de documentació ni tags de versió al repositori.

## Opcions

| Opció | Pros | Contres |
|---|---|---|
| Tags independents per component (`backend-vX`, `android-vX`) | Cicles de release desacoblats | Complexitat per a un projecte d'una sola família; cal saber quina combinació de versions és compatible |
| **Tag únic `vX.Y.Z` per tot el monorepo** | Un sol número de versió pel backend+frontend (una imatge) i l'Android; senzill d'anunciar i de raonar-hi | Backend i Android es "re-versionen" encara que només un dels dos canviï |
| Deixar-ho sense versionar (només `latest`) | Zero feina | No es pot fixar una versió a producció ni oferir un APK descarregable estable |

Es tria **tag únic `vX.Y.Z`**, coherent amb un monorepo petit i un sol equip.

## Decisió

1. **Documentació pública**: lloc estàtic amb **Hugo + tema Docsy** a
   `docs-site/`, publicat a **GitHub Pages** (`https://giro-dev.github.io/fanel/`)
   pel workflow `.github/workflows/docs.yml` en cada push a `main` que toqui
   `docs/`, `docs-site/` o `backend/`. `docs/` (ADRs, `ROADMAP.md`) continua
   sent la font de veritat; `scripts/sync-docs.sh` els sincronitza cap al
   lloc Hugo amb el format esperat (front matter). La pàgina de referència
   API es genera a partir d'un `openapi.json` real, exportat pel backend amb
   `scripts/generate-openapi.sh`.
2. **Imatge Docker**: `docker.yml` continua publicant
   `ghcr.io/giro-dev/fanel:latest` en cada push a `main` (desplegament
   "rolling" per a qui vulgui anar sempre a l'última). `release.yml`, en
   canvi, es dispara amb tags `vX.Y.Z` i publica
   `ghcr.io/giro-dev/fanel:vX.Y.Z` (imatge multi-arquitectura, mateix
   `deploy/Dockerfile`), perquè qui vulgui pugui fixar una versió concreta.
3. **Client Android**: es distribueix com a **APK de debug sense signar**,
   adjunt a la GitHub Release del mateix tag. No es publica a Google Play ni
   es firma amb un keystore de producció: Fanel és d'ús familiar/personal i
   signar/publicar a Play afegeix infraestructura (compte de developer,
   gestió de secrets de signatura) que no aporta valor ara mateix. Es pot
   revisitar quan hi hagi usuaris externs a la llar (vegeu Fase 5 al
   [ROADMAP](../ROADMAP.md)).
4. **Procés de release**: crear un tag `vX.Y.Z` sobre un commit de `main` ja
   verd dispara `release.yml`, que:
   - construeix i testa l'app Android (`assembleDebug testDebugUnitTest`),
   - publica la imatge Docker versionada,
   - crea una GitHub Release amb notes autogenerades i l'APK adjunt.
   El tag es crea i es puja manualment (`git tag vX.Y.Z && git push --tags`);
   no hi ha automatització que decideixi quan pujar de versió.

## Conseqüències

- Per fixar una versió concreta del backend/frontend només cal canviar el
  tag de la imatge (`ghcr.io/giro-dev/fanel:vX.Y.Z`) al `docker-compose.yml`.
- L'APK de debug no és apte per a una distribució pública àmplia (sense
  signatura de release, sense ProGuard/R8); és adequat per a instal·lació
  directa (sideload) dins la família, que és l'ús actual.
- Quan es vulgui signar l'APK (o publicar-lo a Play), caldrà un ADR que
  reemplaci el punt 3 amb la gestió de secrets corresponent (keystore com a
  secret de GitHub, `gradle-play-publisher` o similar).
- La documentació pública depèn de poder arrencar el backend durant el build
  de `docs.yml` per generar `openapi.json`; si això esdevé massa lent,
  caldrà considerar generar l'spec com a part del `mvn verify` del backend i
  publicar-lo com a artefacte en lloc de rearrencar l'app.
