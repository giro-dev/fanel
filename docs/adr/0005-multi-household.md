# ADR 0005 — Multi-household des de l'inici

**Estat**: Acceptat · **Data**: 2026-09-04

## Context
La majoria d'instal·lacions tindran una sola llar, però és probable voler allotjar-hi família extensa o amics. Afegir `householdId` a posteriori implica migrar totes les taules i tots els endpoints.

## Decisió
Model: `Household` → `Member` (rol `ADULT`/`CHILD`, color, PIN opcional per al mode tauleta). **Tota** entitat de negoci pertany a un household i **tota** consulta filtra per household. Els tokens d'API i les credencials MCP tenen àmbit de household.

## Conseqüències
- Cost baix ara (una columna i un filtre), alt després; per això es fa des del dia 1.
- La UI pot amagar el concepte quan només existeix una llar.
