#!/bin/bash
# Desarrollo: carga setenv.sh (si existe) y arranca Spring Boot con Maven.
set -e
cd "$(dirname "$0")"

if [ -f setenv.sh ]; then
  # shellcheck source=/dev/null
  source setenv.sh
  echo "Variables cargadas desde setenv.sh"
else
  echo "Aviso: no hay setenv.sh — usa gemini-local.yml o export GEMINI_APIKEYS"
fi

exec ./mvnw spring-boot:run "$@"
