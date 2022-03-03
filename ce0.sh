#!/usr/bin/env sh
exec java -Xmx50M -Xms50M -ea -cp "$(dirname "$0")"/Ce0.jar MainKt "$@"
