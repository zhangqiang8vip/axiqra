package com.axiqra.start;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InfraConnectionIT {

        private static final String CRDB_URL = "jdbc:postgresql://localhost:26257/axiqra?sslmode=disable";
        private static final String CRDB_USER = "root";
        private static final String CRDB_PASSWORD = "";

        private static final String PG_URL = "jdbc:postgresql://localhost:5433/axiqra_audit_db";
        private static final String PG_USER = "axiqra_audit_app";
        private static final String PG_PASSWORD = "change_me_before_production";

        // ==================== CockroachDB ====================

        @Test
        @DisplayName("CockroachDB: primary datasource reachable via SELECT 1")
        void primaryDataSourceReachable() throws Exception {
                try (Connection conn = DriverManager.getConnection(CRDB_URL, CRDB_USER, CRDB_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT 1")) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                }
        }

        @Test
        @DisplayName("CockroachDB: all 19 tables exist with axiqra_ prefix")
        void cockroachDbTablesExist() throws Exception {
                try (Connection conn = DriverManager.getConnection(CRDB_URL, CRDB_USER, CRDB_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT table_name FROM information_schema.tables " +
                                     "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                                     "ORDER BY table_name")) {
                        List<String> tables = new ArrayList<>();
                        while (rs.next()) tables.add(rs.getString("table_name"));
                        long count = tables.stream().filter(t -> t.startsWith("axiqra_")).count();
                        assertEquals(19, count, "Should have 19 tables with axiqra_ prefix. Found: " + tables);
                }
        }

        @Test
        @DisplayName("CockroachDB: axiqra_review.risk_level has DEFAULT 'R0' and NOT NULL")
        void reviewRiskLevelHasDefault() throws Exception {
                try (Connection conn = DriverManager.getConnection(CRDB_URL, CRDB_USER, CRDB_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT column_default, is_nullable " +
                                     "FROM information_schema.columns " +
                                     "WHERE table_name = 'axiqra_review' AND column_name = 'risk_level'")) {
                        assertTrue(rs.next());
                        assertEquals("'R0'", rs.getString("column_default"),
                                "risk_level should default to 'R0'");
                        assertEquals("NO", rs.getString("is_nullable"),
                                "risk_level should be NOT NULL");
                }
        }

        @Test
        @DisplayName("CockroachDB: insert into axiqra_review without risk_level succeeds with auto-filled R0")
        void insertReviewWithoutRiskLevel() throws Exception {
                long id;
                String riskLevel;
                try (Connection conn = DriverManager.getConnection(CRDB_URL, CRDB_USER, CRDB_PASSWORD);
                     Statement st = conn.createStatement()) {
                        st.execute("DELETE FROM axiqra_review WHERE object_type = 'it_test' AND object_id = 999999");
                        try (ResultSet rs = st.executeQuery(
                                "INSERT INTO axiqra_review(object_type, object_id) " +
                                        "VALUES('it_test', 999999) RETURNING id, risk_level")) {
                                assertTrue(rs.next());
                                id = rs.getLong("id");
                                riskLevel = rs.getString("risk_level");
                        }
                        assertTrue(id > 0, "Insert should return a valid id");
                        assertEquals("R0", riskLevel,
                                "Auto-filled risk_level should be 'R0', got: " + riskLevel);
                        st.execute("DELETE FROM axiqra_review WHERE id = " + id);
                }
        }

        @Test
        @DisplayName("CockroachDB: no legacy axq_ prefixed tables remain")
        void noLegacyTablePrefixes() throws Exception {
                try (Connection conn = DriverManager.getConnection(CRDB_URL, CRDB_USER, CRDB_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT table_name FROM information_schema.tables " +
                                     "WHERE table_schema = 'public' " +
                                     "AND table_type = 'BASE TABLE' " +
                                     "AND table_name LIKE 'axq\\_%'")) {
                        List<String> legacy = new ArrayList<>();
                        while (rs.next()) legacy.add(rs.getString("table_name"));
                        assertTrue(legacy.isEmpty(),
                                "No tables should have axq_ prefix. Found: " + legacy);
                }
        }

        // ==================== PostgreSQL Audit ====================

        @Test
        @DisplayName("PostgreSQL Audit: audit datasource reachable via SELECT 1")
        void auditDataSourceReachable() throws Exception {
                try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT 1")) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                }
        }

        @Test
        @DisplayName("PostgreSQL Audit: all 8 audit tables exist with axiqra_ prefix")
        void auditTablesExist() throws Exception {
                try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT table_name FROM information_schema.tables " +
                                     "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                                     "ORDER BY table_name")) {
                        List<String> tables = new ArrayList<>();
                        while (rs.next()) tables.add(rs.getString("table_name"));
                        assertEquals(8, tables.size(),
                                "Should have exactly 8 audit tables. Found: " + tables);
                        for (String t : tables) {
                                assertTrue(t.startsWith("axiqra_"),
                                        "All tables should use axiqra_ prefix, found: " + t);
                        }
                }
        }

        @Test
        @DisplayName("PostgreSQL Audit: all 8 tables have FORCE ROW LEVEL SECURITY enabled")
        void auditTablesRlsEnabled() throws Exception {
                try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT relname, relforcerowsecurity " +
                                     "FROM pg_class " +
                                     "WHERE relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public') " +
                                     "AND relkind = 'r' " +
                                     "ORDER BY relname")) {
                        int count = 0;
                        while (rs.next()) {
                                assertEquals(true, rs.getBoolean("relforcerowsecurity"),
                                        "Table " + rs.getString("relname") + " should have FORCE RLS enabled");
                                count++;
                        }
                        assertEquals(8, count, "Should have 8 tables with RLS");
                }
        }

        @Test
        @DisplayName("PostgreSQL Audit: each table has 4 RLS policies (SELECT/INSERT/UPDATE/DELETE)")
        void auditTablesHaveFourPolicies() throws Exception {
                try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT COUNT(*) FROM pg_policies WHERE schemaname = 'public'")) {
                        assertTrue(rs.next());
                        assertEquals(32, rs.getInt(1),
                                "Should have 32 policies (4 per table * 8 tables)");
                }
        }

        @Test
        @DisplayName("PostgreSQL Audit: INSERT succeeds, UPDATE and DELETE blocked by RLS")
        void auditAppendOnly() throws Exception {
                long id;
                String requestId = "it_test_" + java.util.UUID.randomUUID();
                try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD);
                     Statement st = conn.createStatement()) {
                        try (ResultSet rs = st.executeQuery(
                                "INSERT INTO axiqra_audit_event(request_id, action) " +
                                        "VALUES('" + requestId + "', 'integration_test') RETURNING id")) {
                                assertTrue(rs.next());
                                id = rs.getLong("id");
                        }
                        assertTrue(id > 0, "INSERT should succeed and return id");

                        try {
                                st.execute("UPDATE axiqra_audit_event SET ip_address = 'blocked' WHERE id = " + id);
                                fail("UPDATE should be blocked by RLS policy");
                        } catch (java.sql.SQLException expected) {
                                assertTrue(expected.getMessage().contains("permission denied") ||
                                                expected.getMessage().contains("rl") ||
                                                "42501".equals(expected.getSQLState()),
                                        "Should throw permission denied, got: " + expected.getMessage());
                        }

                        try {
                                st.execute("DELETE FROM axiqra_audit_event WHERE id = " + id);
                                fail("DELETE should be blocked by RLS policy");
                        } catch (java.sql.SQLException expected) {
                                assertTrue(expected.getMessage().contains("permission denied") ||
                                                expected.getMessage().contains("rl") ||
                                                "42501".equals(expected.getSQLState()),
                                        "Should throw permission denied, got: " + expected.getMessage());
                        }

                        try (ResultSet rs = st.executeQuery(
                                "SELECT 1 FROM axiqra_audit_event WHERE id = " + id)) {
                                assertTrue(rs.next(),
                                        "Row should still exist after blocked UPDATE/DELETE (append-only)");
                        }
                }
        }
}
