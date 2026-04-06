#!/usr/bin/env bash
set -e

YCSB_HOME="../ycsb-0.17.0"
DB="com.yahoo.ycsb.db.Neo4jClient"
CP="$YCSB_HOME/lib/*"
PROPS="neo4j.properties"
THREADS=10
RESULTS_DIR="results"

mkdir -p "$RESULTS_DIR"

echo "============================================"
echo " YCSB Benchmark — Neo4j"
echo " Threads : $THREADS"
echo "============================================"

echo "[LOAD] A inserir registos..."
java -cp "$CP" site.ycsb.Client -load -db "$DB" \
  -P "$PROPS" -threads "$THREADS" -s \
  2>&1 | tee "$RESULTS_DIR/load.txt"

for WL in a b c d e f; do
  echo "[RUN] Workload $WL ..."
  java -cp "$CP" site.ycsb.Client -t -db "$DB" \
    -P "$PROPS" \
    -P "$YCSB_HOME/workloads/workload$WL" \
    -threads "$THREADS" -s \
    2>&1 | tee "$RESULTS_DIR/workload_${WL}.txt"
done

echo "[SCALE] Teste de escalabilidade..."
for T in 1 5 10 20 50; do
  echo "  -> $T thread(s)..."
  java -cp "$CP" site.ycsb.Client -t -db "$DB" \
    -P "$PROPS" \
    -P "$YCSB_HOME/workloads/workloada" \
    -threads "$T" -s \
    2>&1 | tee "$RESULTS_DIR/scale_threads_${T}.txt"
done

echo "============================================"
echo " Concluído! Resultados em: $RESULTS_DIR/"
echo "============================================"
