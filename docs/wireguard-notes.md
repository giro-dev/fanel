# Notes d'implementació — WireGuard transparent (servidor + Android)

Inspecció prèvia del repositori (pas obligatori de l'especificació) i desviacions adoptades.
Les decisions de disseny de l'especificació (WireGuard embegut amb `GoBackend`, túnel dividit per
app, només UDP 51820 exposat, claus privades que no surten del dispositiu, BD com a font de veritat
dels peers) es mantenen totes.

## Estat del servidor

| Aspecte | Trobat |
|---|---|
| Stack | Spring Boot **4.1.1**, **Java 25**, Spring Modulith (un paquet = un mòdul), Maven multi-mòdul (`frontend` + `backend`) |
| Seguretat | Spring Security (`shared/config/SecurityConfig`): **Basic** (admin global per env + membres adults), **Bearer** `fanel.api.token` (`ROLE_API`) i **OIDC** opcional (sessió només per al flux SSO). Stateless. Rols: `ROLE_ADMIN` (admin global o membre ADMIN), `ROLE_API`; `CurrentAccess.hasGlobalAccess()` = admin global o token d'API |
| API | Prefix `/api/v1`, errors RFC 9457 (`ProblemDetail`), OpenAPI generat a `docs/openapi.json` (commitejat) |
| Migracions | **Flyway**, `db/migration/common` (SQL portable) + `postgresql/` i `sqlite/`. Última: `V18` |
| BD | **PostgreSQL i SQLite** suportats (ADR 0003); ids `CHAR(36)` (UUID v7), timestamps `Instant` |
| Desplegament | **Una sola imatge Docker** (`deploy/Dockerfile`, `eclipse-temurin:25-jre-alpine`, usuari no-root `fanel`), compose amb Postgres o SQLite, HTTP **:8080** sense TLS, `ports: 8080:8080` (xarxa bridge) |
| Accés al netns de `wg0` | **No**: el contenidor és en xarxa bridge, sense `NET_ADMIN` ni `wg` a la imatge |
| Scheduling | `@EnableScheduling` ja actiu (`FanelApplication`) |

## Estat del client Android

| Aspecte | Trobat |
|---|---|
| Llenguatge | **Kotlin**, Jetpack Compose, Room, WorkManager |
| SDK | `minSdk 29`, `targetSdk 36`, `compileSdk 36` (no cal *desugaring*: la llibreria `tunnel` demana ≥ 21) |
| HTTP | **Retrofit 2 + OkHttp 4** (`data/remote/CalendarApiFactory.kt` → `ApiFactory`), SSE manual amb OkHttp (`sync/SseHouseholdEvents`) |
| Base URL | `SessionStore.serverUrl` (SharedPreferences), introduïda a mà a `SetupScreen`; `AppContainer.refreshConnection()` reconstrueix les APIs |
| Autenticació | `AuthStore`: capçalera `Basic` o `Bearer` xifrada amb `CredentialCipher` (AES-GCM a Android Keystore) |
| DI | **Manual** (`AppContainer` / `AppContainerContract`, sense Hilt/Koin) |
| Backups | `android:allowBackup="false"`; cleartext permès globalment (`network_security_config`) |
| Tests | JUnit 4 + Robolectric (`./gradlew :app:testDebugUnitTest`); la CI no executa tests Android |

## Desviacions respecte de l'especificació

1. **Rutes sota `/api/v1`** (convenció ADR 0004): `POST /api/v1/admin/wg/pairing-tokens`, `POST /api/v1/wg/enroll`,
   `GET /api/v1/health/identity`, `GET /api/v1/admin/wg/peers`, `DELETE /api/v1/admin/wg/peers/{id}`,
   `DELETE /api/v1/wg/peers/me`. S'afegeix `GET /api/v1/admin/wg/status` (activat, clau pública, interfície accessible) per al panell web.
2. **Propietats `fanel.wireguard.*`** (env `FANEL_WG_*`) en lloc d'`app.wireguard.*`, com la resta de config de Fanel.
   **`enabled: false` per defecte**: les instal·lacions existents no canvien; la validació estricta (`@Validated`) només s'aplica quan està activat.
   Els endpoints existeixen sempre (OpenAPI estable) i responen `404` si WireGuard està desactivat.
   Noves: `psk-encryption-key` (AES-256-GCM per xifrar la PSK en repòs), `reconcile-interval`, `remove-unknown-peers`,
   `enroll-private-only`, `trusted-proxies`, `enforce-tunnel-peer`, `allow-admin-from-tunnel`.
   La configuració s'ha implementat com a JavaBean mutable `@ConfigurationProperties` amb validació agregada explícita només quan `enabled=true`, no com a `record`, per poder mantenir aquesta condicionalitat sense validar els valors buits del mode desactivat.
3. **PostgreSQL i SQLite** (ADR 0003), no només PostgreSQL: migració `V19` a `common` amb tipus portables
   (`CHAR(36)` per als UUID, `TIMESTAMP` com `V18`), sense `DEFAULT now()` (els timestamps els posa l'aplicació amb `Clock`).
   El consum atòmic del token és el mateix `UPDATE … WHERE used_at IS NULL AND expires_at > :now` amb `:now` com a paràmetre.
4. **Assignació d'IP**: en lloc de `FOR UPDATE` (no existeix a SQLite) s'usa un bloqueig dins del procés (Fanel és un sol procés, ADR 0001)
   al voltant de tota la transacció d'emparellament + restricció `UNIQUE(assigned_ip)` com a xarxa de seguretat + un reintent.
5. **`owner_user_id` → `owner_member_id`**: l'emparellament només porta el token, així que el propietari del peer és
   qui va crear el token (`created_by` = membre; `NULL` si és l'admin global o el token d'API).
6. **`DELETE /api/v1/wg/peers/me`**: si la petició arriba pel túnel, el peer s'identifica per la IP d'origen
   (garantida per WireGuard); si arriba per LAN, per la capçalera `X-Fanel-Wg-Public-Key`. Sempre amb l'autenticació normal.
7. **Rols**: "ADMIN" = `CurrentAccess.hasGlobalAccess()` (admin global o token d'API), com la resta d'administració d'instància.
8. **Limitació de ritme** amb un *token bucket* propi per IP (5/min) en lloc de Bucket4j (evita una dependència per a un sol endpoint).
9. **Desplegament (topologia A en Docker)**: la imatge inclou `wg` (`wireguard-tools-wg`) amb `cap_net_admin` com a *file capability*,
   i un override `deploy/docker-compose.wireguard.yml` posa `network_mode: host` + `cap_add: NET_ADMIN` + TLS a `:8443`.
   Així el procés no-root del contenidor veu el `wg0` del host. Per a execució amb JAR al host: helper amb `sudoers` restringit (documentat).
   Continua sent **una sola imatge/contenidor**; la imatge no canvia de comportament si no s'activa.
10. **TLS opcional**: s'activa per entorn (`SERVER_SSL_*`, `SERVER_PORT=8443`) amb un script `scripts/wg-gen-cert.sh`
    (PKCS12 autosignat amb SAN `10.8.0.1` + IP LAN, imprimeix l'empremta SPKI). Les instal·lacions HTTP actuals no canvien.
11. **Panell web d'administració** (no previst a l'especificació): pàgina de la SPA per generar el QR, llistar i revocar peers.
    Sense ella no hi ha on mostrar `qrPngBase64`.
12. **Android**: esquema `fanel://pair?d=<base64url>`; DataStore només per a la configuració WireGuard (la resta de stores segueixen
    amb SharedPreferences); l'emparellament per QR és una opció més de `SetupScreen` (el flux manual d'URL continua existint);
    després de vincular, l'usuari inicia sessió igual que ara (l'autenticació no es toca). URL base lògica: `https://fanel.invalid/`.
13. **Mòdul nou `wireguard`** (Spring Modulith) i ADR 0012 que el documenta.

## Topologia B (alternativa, només documentada)

Vegeu `docs/wireguard.md`, secció "Topologia B".
