# Fanel Android skeleton

Skeleton d'un client Android nadiu per al calendari, alineat amb el backend actual.

## Què inclou

- caché local amb Room
- cua outbox per a mutacions offline
- client Retrofit per al calendari actual (`/api/v1/households/{householdId}/calendar`)
- `CalendarRepository` per a pujada i refresc del rang visible
- `WorkManager` per al sync periòdic amb xarxa

## Configuració ràpida

1. Edita `app/build.gradle.kts` si vols canviar `API_BASE_URL` o `DEFAULT_HOUSEHOLD_ID`.
2. Defineix credencials amb `AuthStore` abans de fer peticions reals; l'esquelet les manté només en memòria.
3. Executa `./gradlew assembleDebug` dins d'`android/`.

## Limitació actual

El backend encara no exposa un endpoint de delta sync ni versions/tombstones. Per això aquest esquelet fa servir outbox local + refresc per rang visible. Si més endavant es vol persistir autenticació, caldrà usar emmagatzematge xifrat basat en Keystore.
