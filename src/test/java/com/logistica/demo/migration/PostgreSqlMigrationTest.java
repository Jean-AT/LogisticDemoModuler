package com.logistica.demo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("logistica_migration_test")
            .withUsername("migration_test")
            .withPassword("migration_test");

    @Test
    void shouldMigrateCleanDatabaseAndUpgradeFromPreviousVersion() {
        Flyway latest = flyway(MigrationVersion.LATEST);
        latest.clean();

        MigrateResult cleanMigration = latest.migrate();
        assertEquals(3, cleanMigration.migrationsExecuted);
        assertTrue(latest.validateWithResult().validationSuccessful);

        latest.clean();
        MigrateResult baselineMigration = flyway(MigrationVersion.fromVersion("1")).migrate();
        assertEquals(1, baselineMigration.migrationsExecuted);

        Flyway upgraded = flyway(MigrationVersion.LATEST);
        MigrateResult upgradeMigration = upgraded.migrate();
        assertEquals(2, upgradeMigration.migrationsExecuted);
        assertTrue(upgraded.validateWithResult().validationSuccessful);
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
