package com.logistica.demo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration/postgresql";
    private static final Set<String> PLATFORM_TABLES = Set.of(
            "companies",
            "organization_units",
            "cost_centers",
            "financing_sources",
            "goals",
            "activities",
            "expense_classifiers",
            "currencies",
            "units_of_measure",
            "catalog_items",
            "users",
            "roles",
            "permissions",
            "role_permissions",
            "user_roles",
            "user_scopes",
            "refresh_tokens",
            "fiscal_periods",
            "document_sequences",
            "audit_events",
            "outbox_events",
            "idempotency_keys");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("logistica_migration_test")
            .withUsername("migration_test")
            .withPassword("migration_test");

    @Test
    void shouldMigrateCleanDatabaseAndUpgradeFromPreviousVersion() throws SQLException {
        Flyway latest = flyway(MigrationVersion.LATEST);
        latest.clean();

        MigrateResult cleanMigration = latest.migrate();
        assertTrue(cleanMigration.migrationsExecuted >= 4);
        assertTrue(latest.validateWithResult().validationSuccessful);
        assertPlatformTablesExist();
        assertPlatformSeedWasMigrated();

        latest.clean();
        MigrateResult baselineMigration = flyway(MigrationVersion.fromVersion("1")).migrate();
        assertEquals(1, baselineMigration.migrationsExecuted);

        Flyway upgraded = flyway(MigrationVersion.LATEST);
        MigrateResult upgradeMigration = upgraded.migrate();
        assertEquals(cleanMigration.migrationsExecuted - 1, upgradeMigration.migrationsExecuted);
        assertTrue(upgraded.validateWithResult().validationSuccessful);
        assertPlatformTablesExist();
        assertPlatformSeedWasMigrated();
    }

    private void assertPlatformTablesExist() throws SQLException {
        Set<String> actualTables = new HashSet<>();
        try (var connection = POSTGRES.createConnection("");
                var tables = connection.getMetaData().getTables(null, "platform", "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                actualTables.add(tables.getString("TABLE_NAME"));
            }
        }
        assertTrue(actualTables.containsAll(PLATFORM_TABLES), () -> "Faltan tablas: " + difference(PLATFORM_TABLES, actualTables));
    }

    private Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private void assertPlatformSeedWasMigrated() throws SQLException {
        try (var connection = POSTGRES.createConnection("");
                var statement = connection.createStatement()) {
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM platform.users WHERE password_hash LIKE '$2a$12$%'")) {
                assertTrue(result.next());
                assertEquals(4, result.getInt(1));
            }
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM logistica_demo.usuarios WHERE password LIKE '{noop}%'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM platform.catalog_items")) {
                assertTrue(result.next());
                assertEquals(10, result.getInt(1));
            }
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM platform.catalog_items WHERE expense_classifier_id IS NULL")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM platform.cost_centers WHERE active = TRUE")) {
                assertTrue(result.next());
                assertEquals(2, result.getInt(1));
            }
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM platform.units_of_measure WHERE active = TRUE")) {
                assertTrue(result.next());
                assertEquals(7, result.getInt(1));
            }
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM platform.catalog_items WHERE item_type = 'SERVICE'")) {
                assertTrue(result.next());
                assertEquals(2, result.getInt(1));
            }
        }
    }

    private Flyway flyway(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(MIGRATION_LOCATION)
                .target(target)
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .baselineOnMigrate(false)
                .outOfOrder(false)
                .cleanDisabled(false)
                .load();
    }
}
