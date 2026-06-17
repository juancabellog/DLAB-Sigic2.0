#!/bin/bash
# Arranca el backend con perfil prod (application-prod.yml).
set -e
cd "$(dirname "$0")"

JAR=$(ls -t target/scientific-products-platform-*.jar 2>/dev/null | grep -v '\.original$' | head -1)
if [ -z "$JAR" ]; then
  echo "No hay JAR en target/. Ejecuta primero: ./build.sh"
  exit 1
fi

echo "Iniciando $JAR con spring.profiles.active=prod"
exec java -jar "$JAR" --spring.profiles.active=prod "$@"
