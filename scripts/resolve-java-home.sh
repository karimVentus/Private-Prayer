#!/usr/bin/env bash
# Use Temurin 21 at ~/jdk21 when shell JAVA_HOME is missing or invalid (e.g. broken lumina symlink).
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
  export JAVA_HOME="$HOME/jdk21"
fi
