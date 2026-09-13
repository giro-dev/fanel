#!/usr/bin/env bash
# Generate an openapi.json snapshot from the real backend and drop it into
# docs-site/static/ so the "Referència API" page can render it with
# @stoplight/elements. Requires JDK 25 and Maven (same as the rest of the
# project); starts the backend briefly against an in-memory SQLite DB.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT_DIR/docs-site/static/openapi.json"
PORT=18080
DB_FILE="$(mktemp -u /tmp/fanel-openapi-XXXXXX.db)"

mkdir -p "$ROOT_DIR/docs-site/static"

echo "Building backend jar..."
mvn -q -B -ntp -pl backend -am package -DskipTests -Dskip.frontend -f "$ROOT_DIR/pom.xml"

JAR="$(find "$ROOT_DIR/backend/target" -maxdepth 1 -name 'fanel-backend-*.jar' ! -name '*.original' | head -1)"
if [ -z "$JAR" ]; then
  echo "Could not find backend jar" >&2
  exit 1
fi

echo "Starting backend on port $PORT..."
FANEL_SQLITE_PATH="$DB_FILE" java -jar "$JAR" \
  --server.port="$PORT" \
  --spring.profiles.active=dev,sqlite \
  > /tmp/fanel-openapi-backend.log 2>&1 &
PID=$!
trap 'kill "$PID" 2>/dev/null || true; rm -f "$DB_FILE"' EXIT

echo "Waiting for backend to become healthy..."
for _ in $(seq 1 60); do
  if curl -sf "http://localhost:$PORT/actuator/health" > /dev/null 2>&1; then
    break
  fi
  sleep 2
done

curl -sf "http://localhost:$PORT/v3/api-docs" -o "$OUT"
echo "Wrote $OUT"
