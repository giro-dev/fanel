#!/usr/bin/env bash
# Sync docs/ (ADRs, ROADMAP) — the binding source of truth per AGENTS.md — into
# docs-site/content, prepending Hugo front matter. Generated files are marked
# and must not be edited directly; re-run this script instead.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADR_SRC="$ROOT_DIR/docs/adr"
ADR_DST="$ROOT_DIR/docs-site/content/ca/docs/adr"
ROADMAP_SRC="$ROOT_DIR/docs/ROADMAP.md"
ROADMAP_DST="$ROOT_DIR/docs-site/content/ca/docs/roadmap"

mkdir -p "$ADR_DST" "$ROADMAP_DST"

weight=10
for src in "$ADR_SRC"/[0-9]*.md; do
  name="$(basename "$src")"
  slug="${name%.md}"
  title="$(sed -n '1p' "$src" | sed -E 's/^#\s*ADR\s*[0-9]+\s*[—-]*\s*//')"
  dst="$ADR_DST/$slug.md"
  {
    echo "---"
    echo "title: \"$title\""
    echo "linkTitle: \"$slug\""
    echo "weight: $weight"
    echo "description: Generat automàticament des de docs/adr/$name. No editis aquest fitxer directament."
    echo "---"
    echo
    tail -n +2 "$src"
  } > "$dst"
  weight=$((weight + 10))
done

{
  echo "---"
  echo "title: \"Full de ruta\""
  echo "linkTitle: \"Roadmap\""
  echo "weight: 50"
  echo "description: Generat automàticament des de docs/ROADMAP.md. No editis aquest fitxer directament."
  echo "---"
  echo
  tail -n +2 "$ROADMAP_SRC"
} > "$ROADMAP_DST/_index.md"

echo "Docs sincronitzats a docs-site/content/ca/docs/{adr,roadmap}"
