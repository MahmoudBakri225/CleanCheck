#!/usr/bin/env sh
# Linux / macOS launcher. Uses the runnable jar when it exists, otherwise Maven.
cd "$(dirname "$0")" || exit 1
if [ -f target/cleancheck.jar ]; then
  exec java -jar target/cleancheck.jar gui
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven was not found in PATH. Install Maven 3.9+ (or build the jar elsewhere) and run again." >&2
  exit 1
fi
exec mvn clean javafx:run
