#!/usr/bin/env bash
# Run MandiLens: ./run.sh backtest | forecast onion Vashi | list | export
set -e
cd "$(dirname "$0")"
[ -d out ] || ./build.sh
java -cp out in.mandilens.MandiLens "$@"
