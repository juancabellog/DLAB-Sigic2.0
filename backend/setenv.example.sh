#!/bin/bash
# Plantilla de variables de entorno para el backend (Gemini, mail, etc.).
#
# Uso (elige una opción):
#
# Opción A — gemini-local.yml (recomendado para ./mvnw spring-boot:run):
#   cp gemini-local.yml.example gemini-local.yml
#   # Edita gemini-local.yml con tus keys (no se sube a git)
#   ./mvnw spring-boot:run
#
# Opción B — setenv.sh + script de arranque:
#   cp setenv.example.sh setenv.sh
#   # Edita setenv.sh
#   ./run-dev.sh
#
# Opción C — export manual en la misma terminal (NO uses ./setenv.sh solo):
#   source setenv.sh    # o:  . ./setenv.sh
#   ./mvnw spring-boot:run
#
# También puedes exportar estas variables en el shell o en el setenv de Tomcat.

# Una sola API key (fallback si GEMINI_APIKEYS no está definido)
# export GEMINI_API_KEY="your-gemini-api-key"

# Pool de keys separadas por coma (rotación ante HTTP 429/503)
# export GEMINI_APIKEYS="key-one,key-two,key-three"

# export GEMINI_MODEL="gemini-flash-lite-latest"
# export GEMINI_TIMEOUT_MS="300000"

# SMTP — requerido para enviar emails (forgot password, etc.)
# export MAIL_HOST="smtp.gmail.com"
# export MAIL_PORT="587"
# export MAIL_USERNAME="user@example.com"
# export MAIL_PASSWORD="your-app-password"
# export MAIL_FROM="noreply@example.com"
# export MAIL_FROM_NAME="Monitoreo Datacenter"
# export FRONTEND_BASE_URL="http://localhost:4200"
