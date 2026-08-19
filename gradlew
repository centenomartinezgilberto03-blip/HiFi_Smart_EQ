#!/bin/sh
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "No Gradle installation is available and this offline archive does not contain gradle-wrapper.jar."
echo "Open the project in Android Studio, or install Gradle 8.7+ and run this script again."
exit 1
