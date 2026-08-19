#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "Compilando y ejecutando ProjectExcelImportTool..."
echo

# macOS may have a JDK installed but not registered with /usr/libexec/java_home.
if [[ -z "${JAVA_HOME:-}" && -d "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Safe by default: the Java tool only writes when --execute is present.
mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=com.sisgic.tools.ProjectExcelImportTool \
  -Dexec.classpathScope=runtime \
  -Dexec.args="$*"
