#!/bin/sh
#
# Gradle wrapper script for Unix
#
set -e
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec "${APP_HOME}/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || true

# Fallback: usa gradle del sistema si no hay jar
GRADLE_EXEC="gradle"
if [ -f "${APP_HOME}/gradle/wrapper/gradle-wrapper.jar" ]; then
  JAVA_EXE="${JAVA_HOME}/bin/java"
  exec "$JAVA_EXE" -jar "${APP_HOME}/gradle/wrapper/gradle-wrapper.jar" "$@"
fi
exec $GRADLE_EXEC "$@"
