package com.logistica.demo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MigrationCatalogTest {

    private static final Path MIGRATION_DIRECTORY =
            Path.of("src", "main", "resources", "db", "migration", "postgresql");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("V([1-9][0-9]*)__([a-z0-9_]+)\\.sql");

    @Test
    void shouldKeepCanonicalMigrationChainContiguousAndNonEmpty() throws IOException {
        List<Path> migrationFiles;
        try (var paths = Files.list(MIGRATION_DIRECTORY)) {
            migrationFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("V"))
                    .toList();
        }

        List<Integer> versions = new ArrayList<>();
        Set<Integer> uniqueVersions = new HashSet<>();
        for (Path migrationFile : migrationFiles) {
            String fileName = migrationFile.getFileName().toString();
            Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
            assertTrue(matcher.matches(), () -> "Nombre Flyway invalido: " + fileName);

            int version = Integer.parseInt(matcher.group(1));
            assertTrue(uniqueVersions.add(version), () -> "Version Flyway duplicada: V" + version);

            String sql = Files.readString(migrationFile).trim();
            assertFalse(sql.isEmpty(), () -> "Migracion vacia: " + fileName);
            assertTrue(sql.contains(";"), () -> "Migracion sin sentencia SQL terminada: " + fileName);
            versions.add(version);
        }

        assertFalse(versions.isEmpty(), "Debe existir al menos una migracion PostgreSQL");
        versions.sort(Integer::compareTo);
        assertEquals(
                java.util.stream.IntStream.rangeClosed(1, versions.get(versions.size() - 1)).boxed().toList(),
                versions,
                "La cadena Flyway debe ser contigua");
    }

    @Test
    void shouldKeepPlatformCatalogSeedAndValidationInV5() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V5__migrate_platform_identity_and_catalog.sql"));

        assertTrue(sql.contains("PLT-T05 validation failed"));
        assertTrue(sql.contains("representative_items_count"));
        assertTrue(sql.contains("catalog_without_classifier_count"));
        assertTrue(sql.contains("SERV-002"));
        assertTrue(sql.contains("META-002"));
        assertTrue(sql.contains("2.3.2.7.11.99"));
    }

    @Test
    void shouldCreateNeedsPlanningSchemaInV6() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V6__create_needs_planning_schema.sql"));

        assertTrue(sql.contains("CREATE SCHEMA cuadronecesidades"));
        assertTrue(sql.contains("cuadronecesidades.needs_plans"));
        assertTrue(sql.contains("cuadronecesidades.need_lines"));
        assertTrue(sql.contains("cuadronecesidades.monthly_needs"));
        assertTrue(sql.contains("ck_monthly_need_month"));
        assertTrue(sql.contains("uk_needs_plan_dimension"));
    }

    @Test
    void shouldAddNeedsWindowsInV7() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V7__add_needs_windows_and_workflow.sql"));

        assertTrue(sql.contains("cuadronecesidades.needs_windows"));
        assertTrue(sql.contains("'REGISTRATION', 'REVIEW', 'CONSOLIDATION'"));
        assertTrue(sql.contains("ck_needs_window_dates"));
        assertTrue(sql.contains("ix_needs_windows_company_year_active"));
    }

    @Test
    void shouldCreateNeedsConsolidationSchemaInV8() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V8__create_needs_consolidation.sql"));

        assertTrue(sql.contains("cuadronecesidades.needs_consolidations"));
        assertTrue(sql.contains("cuadronecesidades.needs_consolidation_sources"));
        assertTrue(sql.contains("cuadronecesidades.needs_consolidation_lines"));
        assertTrue(sql.contains("uk_needs_consolidation_open"));
        assertTrue(sql.contains("'CONSOLIDATED', 'REVERSED', 'TRANSFERRED'"));
    }

    @Test
    void shouldTrackNeedsTransferResultInV9() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V9__add_needs_transfer_tracking.sql"));

        assertTrue(sql.contains("transfer_id"));
        assertTrue(sql.contains("unit_budget_exercise_id"));
        assertTrue(sql.contains("ck_needs_consolidation_transfer_result"));
        assertTrue(sql.contains("ix_need_lines_available"));
    }

    @Test
    void shouldCreateBudgetFoundationsInV10() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V10__create_budget_foundations.sql"));

        assertTrue(sql.contains("CREATE SCHEMA presupuesto"));
        assertTrue(sql.contains("presupuesto.budget_exercises"));
        assertTrue(sql.contains("presupuesto.budget_ceilings"));
        assertTrue(sql.contains("presupuesto.budget_lines"));
        assertTrue(sql.contains("presupuesto.budget_movements"));
        assertTrue(sql.contains("'UNIDADES', 'PIA', 'PIM'"));
        assertTrue(sql.contains("ck_budget_line_amounts"));
        assertTrue(sql.contains("uk_budget_movement_idempotency"));
    }

    @Test
    void shouldReceiveNeedsTransfersInV11() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V11__receive_needs_transfers.sql"));

        assertTrue(sql.contains("presupuesto.budget_transfers"));
        assertTrue(sql.contains("presupuesto.budget_transfer_lines"));
        assertTrue(sql.contains("uk_budget_transfer_consolidation"));
        assertTrue(sql.contains("uk_budget_transfer_line"));
        assertTrue(sql.contains("ix_budget_transfer_lines_needs"));
    }

    @Test
    void shouldRegisterBudgetPlanReviewsInV12() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V12__create_budget_plan_review.sql"));

        assertTrue(sql.contains("presupuesto.budget_plan_reviews"));
        assertTrue(sql.contains("uk_budget_plan_review_exercise"));
        assertTrue(sql.contains("fk_budget_plan_review_exercise"));
        assertTrue(sql.contains("ix_budget_plan_reviews_reviewed_at"));
    }

    @Test
    void shouldCreateBudgetControlsInV13() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V13__create_budget_controls.sql"));

        assertTrue(sql.contains("presupuesto.budget_controls"));
        assertTrue(sql.contains("presupuesto.budget_control_lines"));
        assertTrue(sql.contains("uk_budget_control_active_precommit"));
        assertTrue(sql.contains("ck_budget_control_line_amounts"));
        assertTrue(sql.contains("ix_budget_control_lines_budget_line"));
    }

    @Test
    void shouldCreateLogisticsModelAndMigrateLegacyInV14() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V14__create_logistics_model_and_migrate_legacy.sql"));

        assertTrue(sql.contains("CREATE SCHEMA logistica"));
        assertTrue(sql.contains("logistica.requisitions"));
        assertTrue(sql.contains("logistica.requisition_lines"));
        assertTrue(sql.contains("logistica.purchase_orders"));
        assertTrue(sql.contains("logistica.inventory_movements"));
        assertTrue(sql.contains("'LEGACY_DEMO'"));
        assertTrue(sql.contains("LOG-T02 validation failed"));
    }

    @Test
    void shouldLinkLegacyRequisitionsToNeedsInV15() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V15__link_legacy_requisitions_to_needs.sql"));

        assertTrue(sql.contains("logistica_demo.requerimientos"));
        assertTrue(sql.contains("needs_plan_id"));
        assertTrue(sql.contains("needs_line_id"));
        assertTrue(sql.contains("available_quantity_snapshot"));
        assertTrue(sql.contains("logistica.requisition_need_traceability"));
        assertTrue(sql.contains("LOG-T03 validation failed"));
    }

    @Test
    void shouldLinkRequisitionsToBudgetControlsInV16() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V16__link_requisitions_to_budget_controls.sql"));

        assertTrue(sql.contains("budget_control_id"));
        assertTrue(sql.contains("presupuesto.budget_controls"));
        assertTrue(sql.contains("logistica.requisition_budget_traceability"));
        assertTrue(sql.contains("LOG-T04 validation failed"));
    }
}
