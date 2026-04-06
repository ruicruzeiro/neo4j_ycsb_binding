package com.yahoo.ycsb.db;

import site.ycsb.*;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;

/**
 * YCSB binding for Neo4j.
 *
 * Configurable properties (neo4j.properties):
 *   neo4j.url      - Bolt URL (default: bolt://localhost:7687)
 *   neo4j.user     - username (default: neo4j)
 *   neo4j.password - password (default: password)
 *   neo4j.database - database name (default: neo4j)
 */
public class Neo4jClient extends DB {

    private Driver driver;
    private String database;

    // ------------------------------------------------------------------ init

    @Override
    public void init() throws DBException {
        Properties props = getProperties();

        String url      = props.getProperty("neo4j.url",      "bolt://localhost:7687");
        String user     = props.getProperty("neo4j.user",     "neo4j");
        String password = props.getProperty("neo4j.password", "password");
        database        = props.getProperty("neo4j.database", "neo4j");

        try {
            driver = GraphDatabase.driver(url, AuthTokens.basic(user, password));
            driver.verifyConnectivity();
            System.out.println("[Neo4jClient] Connected to " + url);
        } catch (Exception e) {
            throw new DBException("Failed to connect to Neo4j: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() throws DBException {
        if (driver != null) {
            driver.close();
        }
    }

    // ------------------------------------------------------------------ READ

    // Reads a single node by its id. Returns the requested fields, or all fields if none specified.
    @Override
    public Status read(String table, String key,
                       Set<String> fields, Map<String, ByteIterator> result) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {

            String query = "MATCH (n:" + table + " {id: $key}) RETURN n";
            Result r = session.run(query, Values.parameters("key", key));

            if (!r.hasNext()) {
                return Status.NOT_FOUND;
            }

            Record record = r.next();
            Map<String, Object> props = record.get("n").asNode().asMap();

            for (Map.Entry<String, Object> entry : props.entrySet()) {
                if (fields == null || fields.contains(entry.getKey())) {
                    result.put(entry.getKey(),
                               new StringByteIterator(entry.getValue().toString()));
                }
            }
            return Status.OK;

        } catch (Exception e) {
            System.err.println("[READ] Error: " + e.getMessage());
            return Status.ERROR;
        }
    }

    // ------------------------------------------------------------------ SCAN

    // Reads a range of nodes starting from startKey, ordered by id, up to recordCount nodes.
    @Override
    public Status scan(String table, String startKey, int recordCount,
                       Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {

            String query = "MATCH (n:" + table + ") " +
                           "WHERE n.id >= $startKey " +
                           "RETURN n ORDER BY n.id LIMIT $limit";

            Result r = session.run(query,
                Values.parameters("startKey", startKey, "limit", recordCount));

            while (r.hasNext()) {
                Record record = r.next();
                Map<String, Object> props = record.get("n").asNode().asMap();
                HashMap<String, ByteIterator> row = new HashMap<>();

                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (fields == null || fields.contains(entry.getKey())) {
                        row.put(entry.getKey(),
                                new StringByteIterator(entry.getValue().toString()));
                    }
                }
                result.add(row);
            }
            return Status.OK;

        } catch (Exception e) {
            System.err.println("[SCAN] Error: " + e.getMessage());
            return Status.ERROR;
        }
    }

    // ----------------------------------------------------------------- UPDATE

    // Updates one or more properties of an existing node.
    @Override
    public Status update(String table, String key, Map<String, ByteIterator> values) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {

            // Build "n.field1 = $field1, n.field2 = $field2, ..."
            StringBuilder sets = new StringBuilder();
            Map<String, Object> params = new HashMap<>();
            params.put("key", key);

            for (String field : values.keySet()) {
                if (sets.length() > 0) sets.append(", ");
                sets.append("n.").append(field).append(" = $").append(field);
                params.put(field, values.get(field).toString());
            }

            String query = "MATCH (n:" + table + " {id: $key}) SET " + sets;
            session.run(query, params).consume();
            return Status.OK;

        } catch (Exception e) {
            System.err.println("[UPDATE] Error: " + e.getMessage());
            return Status.ERROR;
        }
    }

    // ----------------------------------------------------------------- INSERT

    // Creates a new node with the given properties.
    @Override
    public Status insert(String table, String key, Map<String, ByteIterator> values) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {

            Map<String, Object> params = new HashMap<>();
            params.put("key", key);

            Map<String, Object> props = new HashMap<>();
            props.put("id", key);
            for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
                props.put(entry.getKey(), entry.getValue().toString());
            }
            params.put("props", props);

            String query = "CREATE (n:" + table + ") SET n = $props";
            session.run(query, params).consume();
            return Status.OK;

        } catch (Exception e) {
            System.err.println("[INSERT] Error: " + e.getMessage());
            return Status.ERROR;
        }
    }

    // ----------------------------------------------------------------- DELETE

    // Deletes a node by its id.
    @Override
    public Status delete(String table, String key) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {

            String query = "MATCH (n:" + table + " {id: $key}) DELETE n";
            session.run(query, Values.parameters("key", key)).consume();
            return Status.OK;

        } catch (Exception e) {
            System.err.println("[DELETE] Error: " + e.getMessage());
            return Status.ERROR;
        }
    }
}
