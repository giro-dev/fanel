# ADR 0013 — Catàleg de la compra bilingüe (català/castellà)

**Estat**: Acceptat · **Data**: 2026-10-19

## Context

Els ítems de la compra guardaven un sol `name` i una `category` de text lliure. En una llar on una persona fa servir l'app en català i una altra en castellà, "Llet" i "Leche" eren dos ítems diferents i la llista s'omplia de duplicats; les categories, a més, eren una llista fixa en català al frontend.

## Opcions avaluades

| Opció | Pros | Contres |
|---|---|---|
| **Catàleg de productes i categories amb nom `ca` + `es`**; l'ítem referencia el producte | Un sol registre per producte; cada client el veu en el seu idioma; permet sembrar un catàleg inicial | Taules noves i backfill dels ítems existents |
| Traduir el text de l'ítem en desar-lo (LLM) | Sense catàleg | Depèn d'un proveïdor d'IA (cap actiu per defecte, ADR 0006); no resol la identitat |
| Normalitzar a un sol idioma de la llar | Molt simple | Una de les dues persones veu la llista en un idioma que no és el seu |

## Decisió

S'adopta l'opció **1**.

1. Taules `shopping_category` i `shopping_product` amb `name_ca` i `name_es`. `household_id NULL` = catàleg compartit sembrat per la migració `V20` (només lectura); la resta són productes/categories propis de la llar.
2. `shopping_item` passa a tenir `product_id` i `category_id`. Es conserven `name`/`category` de text com a fallback i per compatibilitat.
3. Idioma de resposta: `Accept-Language` (`ca`/`es`) de la petició; si no n'hi ha (esdeveniments, assistent), el `locale` de la llar; si no, `ca`. `name` i `category` dels DTOs ja surten localitzats; s'hi afegeixen `productId` i `categoryId`.
4. En afegir un ítem, el nom es busca al catàleg en tots dos idiomes (sense accents, majúscules ni `l·l`, i tolerant plurals simples). Si la llista ja té aquest producte, es reutilitza l'ítem (torna a pendent) en lloc de crear-ne un de nou.
5. Un nom desconegut crea un producte de la llar amb el mateix text en tots dos idiomes (`translated = false`) i publica `ShoppingProductCreated`. Si l'agent intern `product-translator` és actiu, el mòdul `assistant` escolta l'esdeveniment, en demana la traducció i la categoria i les desa amb `ShoppingApi.updateProduct`. Sense IA, també es poden corregir via `PATCH /shopping/products/{id}`.
6. En arrencar, `ShoppingCatalogBackfill` enllaça els ítems antics amb el catàleg i fusiona els duplicats pendents d'una mateixa llista.

## Conseqüències

- Els clients han d'enviar `Accept-Language` (web: idioma de i18next; Android: locale del dispositiu).
- Les categories del formulari i els suggeriments de producte surten de `GET /shopping/categories` i `/shopping/products`.
- Afegir productes al catàleg compartit = nova migració Flyway.
