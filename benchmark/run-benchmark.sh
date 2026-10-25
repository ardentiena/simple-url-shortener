#!/usr/bin/env bash
set -euo pipefail

run_label="${1:?usage: benchmark/run-benchmark.sh <run-label>}"
results_directory="benchmark/results"
heartbeat_file="${results_directory}/heartbeat-${run_label}.txt"
vmstat_file="${results_directory}/vmstat-${run_label}.txt"

mkdir -p "${results_directory}"

( while true; do date +%s.%N; sleep 0.5; done ) > "${heartbeat_file}" &
heartbeat_pid=$!
vmstat -t 1 > "${vmstat_file}" &
vmstat_pid=$!

cleanup() {
  kill "${heartbeat_pid}" "${vmstat_pid}" 2>/dev/null || true
}
trap cleanup EXIT

k6 run --summary-export="${results_directory}/${run_label}.json" benchmark/redirect-load-test.js

cleanup
echo "Heartbeat gaps longer than 2 seconds (no lines below means no freeze):"
awk 'NR > 1 { gap_seconds = $1 - previous_timestamp; if (gap_seconds > 2) printf "gap of %.1f s before line %d\n", gap_seconds, NR } { previous_timestamp = $1 }' "${heartbeat_file}"
