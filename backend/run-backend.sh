#!/bin/bash
cd "$(dirname "$0")"
if [ -f setenv.sh ]; then
  # shellcheck source=/dev/null
  source setenv.sh
fi
./mvnw spring-boot:run > backend.log 2>&1 &
