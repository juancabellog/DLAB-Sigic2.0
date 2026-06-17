#!/bin/bash
# Empaqueta el JAR. Incluye application.yml y application-prod.yml.
# El perfil Spring "prod" NO se elige aquí: se activa al arrancar el JAR (ver run-prod.sh).
set -e
cd "$(dirname "$0")"
./mvnw clean package -DskipTests "$@"
