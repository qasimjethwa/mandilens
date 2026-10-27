#!/usr/bin/env bash
# Compile MandiLens (pure Java 21, zero dependencies).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac -d out $(find src -name '*.java')
echo "Compiled OK -> out/"
