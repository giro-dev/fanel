#!/usr/bin/env bash
# Regenerates docs-site content from docs/. Edit the sources under docs/,
# never the generated files in docs-site/content/.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/docs"
OUT="$ROOT/docs-site/content/ca/docs"
REPO_URL="https://github.com/giro-dev/fanel"
PRIVACY_POLICY_SOURCE="privacy-policy.md"

mkdir -p "$OUT/adr" "$OUT/roadmap"

# Strip the H1 heading (the front matter title replaces it) and a leading blank line.
strip_h1() { tail -n +2 | sed '1{/^$/d}'; }

# Rewrite repo-relative markdown links to site-relative URLs (or GitHub).
fix_links() {
  case "$1" in
    adr) # page at /docs/adr/<slug>/; siblings live one level up
      sed -E 's|\]\((adr/)?([0-9]{4}[^)/]*)\.md\)|](../\2/)|g' ;;
    roadmap) # page at /docs/roadmap/
      sed -E 's|\]\(adr/([0-9]{4}[^)/]*)\.md\)|](../adr/\1/)|g
              s|\]\(adr/\)|](../adr/)|g
              s|\]\(PLA-ORIGINAL\.md\)|](pla-original/)|g
              s|\]\(ROADMAP\.md\)|](.)|g' ;;
    pla) # page at /docs/roadmap/pla-original/
      sed -E 's|\]\(adr/([0-9]{4}[^)/]*)\.md\)|](../../adr/\1/)|g
              s|\]\(adr/\)|](../../adr/)|g
              s|\]\(ROADMAP\.md\)|](../)|g
              s|\]\(PLA-ORIGINAL\.md\)|](.)|g' ;;
  esac | sed -E "s|\]\(\.\./AGENTS\.md\)|]($REPO_URL/blob/main/AGENTS.md)|g"
}

for src in "$SRC"/adr/[0-9][0-9][0-9][0-9]-*.md; do
  name="$(basename "$src" .md)"
  title="$(sed -n 's/^# ADR [0-9]* — //p' "$src" | head -1)"
  {
    printf -- "---\ntitle: \"%s\"\nlinkTitle: \"%s\"\nweight: %d\ndescription: Generat automàticament des de docs/adr/%s.md. No editis aquest fitxer directament.\n---\n\n" \
      "$title" "$name" "$((10#${name%%-*} * 10))" "$name"
    strip_h1 < "$src" | fix_links adr
  } > "$OUT/adr/$name.md"
done

{
  printf -- "---\ntitle: \"Full de ruta\"\nlinkTitle: \"Roadmap\"\nweight: 50\ndescription: Generat automàticament des de docs/ROADMAP.md. No editis aquest fitxer directament.\n---\n\n"
  strip_h1 < "$SRC/ROADMAP.md" | fix_links roadmap
} > "$OUT/roadmap/_index.md"

{
  printf -- "---\ntitle: \"Pla original\"\nlinkTitle: \"PLA-ORIGINAL\"\nweight: 60\ndescription: Generat automàticament des de docs/PLA-ORIGINAL.md. No editis aquest fitxer directament.\n---\n\n"
  strip_h1 < "$SRC/PLA-ORIGINAL.md" | fix_links pla
} > "$OUT/roadmap/pla-original.md"

{
  printf -- "---\ntitle: \"Privacy Policy\"\nlinkTitle: \"Privacitat\"\nweight: 70\ndescription: Generat automàticament des de docs/%s. No editis aquest fitxer directament.\n---\n\n" \
    "$PRIVACY_POLICY_SOURCE"
  strip_h1 < "$SRC/$PRIVACY_POLICY_SOURCE"
} > "$OUT/privacy-policy.md"

echo "Synced docs -> $OUT"
