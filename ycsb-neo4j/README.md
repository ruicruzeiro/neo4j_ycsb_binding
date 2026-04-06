# YCSB Binding para Neo4j

Binding simples do YCSB para Neo4j, desenvolvido para avaliação experimental
no âmbito de trabalho académico de Big Data.

## Estrutura

```
ycsb-neo4j/
├── pom.xml                          # Build Maven
├── neo4j.properties                 # Configuração do binding e workload
├── run_benchmark.sh                 # Script que corre todos os benchmarks
├── parse_results.py                 # Gera gráficos a partir dos resultados
└── src/main/java/com/yahoo/ycsb/db/
    └── Neo4jClient.java             # Binding (READ, INSERT, UPDATE, DELETE, SCAN)
```

---

## Passo 1 — Pré-requisitos

```bash
# Java 11+
java -version

# Maven
mvn -version

# Python (para gráficos)
pip install matplotlib
```

---

## Passo 2 — Compilar o binding

```bash
cd ycsb-neo4j

# Descarrega o YCSB core para o repositório local Maven
mvn install:install-file \
  -Dfile=ycsb-0.17.0/lib/core-0.17.0.jar \
  -DgroupId=site.ycsb \
  -DartifactId=core \
  -Dversion=0.17.0 \
  -Dpackaging=jar

# Compila e cria o fat jar
mvn clean package -DskipTests
```

O jar resultante estará em:
`target/neo4j-binding-1.0-SNAPSHOT-jar-with-dependencies.jar`

---

## Passo 3 — Instalar o YCSB

```bash
curl -O --location \
  https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz

tar xfvz ycsb-0.17.0.tar.gz

# Copiar o binding para a pasta lib do YCSB
cp target/neo4j-binding-1.0-SNAPSHOT-jar-with-dependencies.jar \
   ycsb-0.17.0/lib/
```

---

## Passo 4 — Configurar neo4j.properties

Edita `neo4j.properties` conforme necessário:

```properties
neo4j.url=bolt://localhost:7687
neo4j.user=neo4j
neo4j.password=password
neo4j.database=neo4j
recordcount=10000
operationcount=10000
```

---

## Passo 5 — Criar índice no Neo4j (importante para performance!)

No Neo4j Browser (http://localhost:7474):

```cypher
CREATE INDEX usertable_id IF NOT EXISTS FOR (n:usertable) ON (n._id);
```

---

## Passo 6 — Correr os benchmarks

```bash
chmod +x run_benchmark.sh
YCSB_HOME=./ycsb-0.17.0 ./run_benchmark.sh
```

Os resultados ficam em `results/`.

### Ou manualmente, workload a workload:

```bash
# Load
./ycsb-0.17.0/bin/ycsb.sh load com.yahoo.ycsb.db.Neo4jClient \
  -P neo4j.properties -threads 10 -s

# Run Workload A
./ycsb-0.17.0/bin/ycsb.sh run com.yahoo.ycsb.db.Neo4jClient \
  -P neo4j.properties \
  -P ycsb-0.17.0/workloads/workloada \
  -threads 10 -s
```

---

## Passo 7 — Gerar gráficos

```bash
python3 parse_results.py
```

Produz:
- `throughput_workloads.png` — throughput (ops/sec) por workload
- `latency_workloads.png`   — latência p99 por workload
- `scalability.png`         — escalabilidade (threads vs throughput)

---

## Operações implementadas

| Operação | Cypher gerado |
|----------|---------------|
| READ     | `MATCH (n:table {_id: $key}) RETURN n` |
| INSERT   | `CREATE (n:table) SET n = $props` |
| UPDATE   | `MATCH (n:table {_id: $key}) SET n.field = $val` |
| DELETE   | `MATCH (n:table {_id: $key}) DELETE n` |
| SCAN     | `MATCH (n:table) WHERE n._id >= $key RETURN n ORDER BY n._id LIMIT $n` |

---

## Workloads YCSB

| Workload | Read | Update | Insert | Scan | Descrição |
|----------|------|--------|--------|------|-----------|
| A        | 50%  | 50%    | —      | —    | Update heavy |
| B        | 95%  | 5%     | —      | —    | Read mostly |
| C        | 100% | —      | —      | —    | Read only |
| D        | 95%  | —      | 5%     | —    | Read latest |
| E        | —    | —      | 5%     | 95%  | Short ranges |
| F        | 50%  | —      | —      | —    | Read-modify-write |
