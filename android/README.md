# Fanel Android skeleton

Skeleton d'un client Android nadiu per al calendari, alineat amb el backend actual.

## Què inclou

- caché local amb Room
- cua outbox per a mutacions offline
- client Retrofit per al calendari actual (`/api/v1/households/{householdId}/calendar`)
- `CalendarRepository` per a pujada i refresc del rang visible
- `WorkManager` per al sync periòdic amb xarxa
- UI en Jetpack Compose amb vistes de dia, setmana i mes, creació i esborrat d'esdeveniments
- vista de receptes (`/api/v1/households/{householdId}/recipes`) amb cerca, detall, creació i esborrat
- vista de menú setmanal (`/api/v1/households/{householdId}/menu`) amb edició d'àpats i enllaç a receptes
- vista de llista de la compra (`/api/v1/households/{householdId}/shopping`) amb múltiples llistes, categories, recurrents i esborrat de comprats
- vista de tasques (`/api/v1/households/{householdId}/chores`) amb assignació a membres, venciment i recurrència amb rotació
- xat amb l'assistent (`/api/v1/households/{householdId}/assistant`) amb selecció d'agent, adjunts d'imatge i creació de receptes des de la resposta
- selector "Qui ets?" a la configuració (membre actiu amb PIN opcional, usat per atribuir el xat de l'assistent)
- refresc en temps real via SSE (`/api/v1/events`) per a calendari, menú, compra i tasques
- indicador de connectivitat ("Sense connexió") a totes les pantalles

## Configuració ràpida

1. A la primera arrencada, la pantalla de setup demana la URL del servidor i credencials (usuari + contrasenya, o token d'API) i després deixa triar la llar.
2. Les credencials es guarden xifrades amb Android Keystore (`CredentialCipher`); la sessió (URL + llar) a `SessionStore`.
3. Executa `./gradlew assembleDebug` dins d'`android/`. `API_BASE_URL` a `app/build.gradle.kts` només és la URL per defecte del formulari de setup.

## Build de release

`./gradlew assembleRelease` genera un APK signat amb la clau self-signed de dev (`app/selfsigned.jks`). Per signar amb una clau pròpia, defineix les variables d'entorn `FANEL_KEYSTORE`, `FANEL_KEYSTORE_PASSWORD`, `FANEL_KEY_ALIAS` i `FANEL_KEY_PASSWORD`. La pipeline de release (`release.yml`) fa el mateix via secrets de GitHub.

## Limitació actual

El backend encara no exposa un endpoint de delta sync ni versions/tombstones. Per això aquest esquelet fa servir outbox local + refresc per rang visible. Si més endavant es vol persistir autenticació, caldrà usar emmagatzematge xifrat basat en Keystore.
