# YCSB Binding for Neo4j

A custom YCSB binding for Neo4j, developed as part of a Big Data course practical assignment at ISEC (Master in Computer Engineering).
Since YCSB does not include an official binding for Neo4j, this project implements one by extending the site.ycsb.DB abstract class. The binding is also compatible with Memgraph, as both databases use the Bolt protocol and the Neo4j Java Driver.
Requirements

Java 11+

Apache Maven 3

YCSB 0.17.0

Neo4j 5 or Memgraph (via Docker)

Setup
1. Download YCSB:
   
`bashcurl -L -O https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz`
`tar xfvz ycsb-0.17.0.tar.gz`

3. Build the binding:
   
`bashmvn install:install-file \
  -Dfile=../ycsb-0.17.0/lib/core-0.17.0.jar \
  -DgroupId=site.ycsb -DartifactId=core \
  -Dversion=0.17.0 -Dpackaging=jar`
`mvn clean package -DskipTests
cp target/neo4j-binding-*-jar-with-dependencies.jar ../ycsb-0.17.0/lib/`

5. Configure neo4j.properties:

`propertiesneo4j.url=bolt://localhost:7687
neo4j.user=neo4j
neo4j.password=password
neo4j.database=neo4j`

For Memgraph, use memgraph.properties:

`propertiesneo4j.url=bolt://localhost:7688
neo4j.user=
neo4j.password=
neo4j.database=memgraph`

Running the benchmark

Load phase:

`java -cp "../ycsb-0.17.0/lib/*" site.ycsb.Client -load \
  -db com.yahoo.ycsb.db.Neo4jClient \
  -P neo4j.properties \
  -p recordcount=10000 \
  -threads 10 -s`
  
Run phase (Workload A):

`java -cp "../ycsb-0.17.0/lib/*" site.ycsb.Client -t \
  -db com.yahoo.ycsb.db.Neo4jClient \
  -P neo4j.properties \
  -P ../ycsb-0.17.0/workloads/workloada \
  -p recordcount=10000 \
  -p operationcount=100000 \
  -threads 10 -s`
  
Supported Operations

READ
`MATCH (n:Movie {id: $key}) RETURN n`

INSERT
`CREATE (n:Movie) SET n = $props`

UPDATE
`MATCH (n:Movie {id: $key}) SET n.field = $val`

DELETE
`MATCH (n:Movie {id: $key}) DELETE n`

SCAN
`MATCH (n:Movie) WHERE n.id >= $key RETURN n ORDER BY n.id LIMIT $count`
