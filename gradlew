#!/bin/sh
set -eu
GRADLE_VERSION=9.5.1
BASE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/.gradle-bootstrap"
ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_HOME="$BASE/gradle-$GRADLE_VERSION"
GRADLE="$GRADLE_HOME/bin/gradle"
if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install JDK 25 first." >&2
  exit 1
fi
if [ ! -x "$GRADLE" ]; then
  mkdir -p "$BASE"
  echo "Gradle $GRADLE_VERSION is not installed for this project. Downloading it now..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    echo "ERROR: curl or wget is required for the first build." >&2
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$GRADLE" "$@"
