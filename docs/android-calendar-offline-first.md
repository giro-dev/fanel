# Esquelet Android offline-first per al calendari

Aquest document adapta l'esquelet de sincronització offline-first al **contracte REST real** del calendari a Fanel avui.

## Resum del backend actual

El backend del mòdul `calendar` ja exposa CRUD sota `'/api/v1/households/{householdId}/calendar'` amb aquestes característiques:

- `GET /api/v1/households/{householdId}/calendar` retorna `CalendarEventDto` i **expandeix recurrències** dins del rang `from` / `to`.
- `POST /api/v1/households/{householdId}/calendar` crea una sèrie d'esdeveniment i el servidor genera l'UUID v7.
- `PATCH /api/v1/households/{householdId}/calendar/{eventId}` actualitza la sèrie.
- `DELETE /api/v1/households/{householdId}/calendar/{eventId}` fa **esborrat físic**.
- Autenticació disponible per a clients via **Basic auth** o token d'API a l'header `Authorization`; a Android és preferible token d'API i qualsevol persistència futura s'hauria de fer amb emmagatzematge xifrat basat en Keystore.

DTO actual del backend:

```java
public record CalendarEventDto(
    UUID id,
    UUID householdId,
    String title,
    LocalDate date,
    LocalDate anchorDate,
    LocalTime time,
    UUID addedBy,
    List<UUID> assigneeIds,
    RecurrenceFrequency recurrenceFreq,
    Integer recurrenceInterval,
    LocalDate recurrenceUntil
) {}
```

## Diferències respecte a l'esquema de delta sync proposat

L'esquelet original assumia un backend amb:

- ID generat pel client
- `@Version`
- `updatedAt`
- `deletedAt`
- endpoints dedicats de sync per deltes

Això **encara no existeix** al backend actual. Per tant, el client Android d'aquest PR aplica una adaptació temporal:

1. Manté un **ID local** per treballar offline i un `remoteId` nullable fins que el `POST` del servidor respon.
2. Manté una **outbox local** per diferir `CREATE`, `UPDATE` i `DELETE`.
3. Fa **pujada** amb els endpoints CRUD actuals.
4. Fa **baixada** amb un `GET` de rang visible i refresca la caché local de les ocurrències sincronitzades.
5. Deixa encapsulada la sincronització a `CalendarRepository` per poder migrar més endavant a `GET/POST /api/sync/calendar-events` sense reescriure la resta de capes.

## Adaptació de model al backend actual

### Entitat local Room

Com que el `GET` actual retorna **ocurrències expandides** i no una sèrie crua, la caché local guarda files identificades per `occurrenceKey = <eventId>:<date>`.

Això permet:

- pintar correctament vistes mensuals/setmanals
- conservar `anchorDate` per saber quin és l'inici real de la sèrie
- continuar editant o esborrant via `remoteId`

Per a esdeveniments creats offline:

- `localId` és un UUID local
- `remoteId` és `null` fins al primer sync
- `occurrenceKey` inicial és el `localId`

### Estratègia de sync actual

#### Pujada

- `CREATE` → `POST /api/v1/households/{householdId}/calendar`
- `UPDATE` → `PATCH /api/v1/households/{householdId}/calendar/{remoteId}`
- `DELETE` → `DELETE /api/v1/households/{householdId}/calendar/{remoteId}`

La outbox es **compacta per `localId`** abans d'enregistrar una nova mutació, de manera que només queda l'últim estat pendent de cada esdeveniment.

#### Baixada

- El client demana `GET /api/v1/households/{householdId}/calendar?from=...&to=...`
- El backend retorna ocurrències ja expandides
- La caché local reemplaça només les files sincronitzades del rang visible
- Les files locals pendents (`PENDING_*`) es mantenen intactes fins que es puguin pujar

## Limitacions conegudes

- Sense `updatedAt` ni `@Version`, **no hi ha detecció formal de conflictes**.
- Sense `deletedAt`, el client no pot rebre tombstones; els esborrats remots només es reflecteixen quan es refresca el rang afectat.
- Com que el `GET` retorna ocurrències, la caché local és una **read model** per a UI, no un mirror 1:1 de la taula JPA del servidor.

## Fitxers de l'esquelet Android

El projecte nadiu s'ha afegit a `android/` i inclou:

- configuració Gradle Android
- `Room` (`CalendarEventDao`, `OutboxDao`, `FanelDatabase`)
- `Retrofit` (`CalendarApi`, `CalendarEventDto`, requests)
- `CalendarRepository` amb outbox + refresh de rang
- `SyncWorker` i `CalendarSyncScheduler`
- una `MainActivity` mínima per demostrar el cablejat bàsic

## Proper pas quan el backend incorpori delta sync real

Quan existeixin `version`, `updatedAt`, `deletedAt` i un endpoint dedicat de sync, el canvi principal quedarà confinat a:

- `CalendarApi`
- `CalendarRepository.pushPendingChanges()`
- `CalendarRepository.pullVisibleRange()`
- el model local (si es vol passar d'ocurrències a sèries + tombstones)
