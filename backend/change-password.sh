#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "Compilando y ejecutando ChangeUserPasswordTool..."
echo

mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=com.sisgic.tools.ChangeUserPasswordTool \
  -Dexec.classpathScope=runtime \
  "$@"
