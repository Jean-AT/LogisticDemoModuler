package com.logistica.demo.presupuesto.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.demo.platform.infrastructure.persistence.JdbcIdempotencyAdapter;
import com.logistica.demo.presupuesto.api.BudgetTransferResult;
import com.logistica.demo.presupuesto.api.TransferNeedsCommand;
import com.logistica.demo.presupuesto.api.TransferredNeedLine;
import com.logistica.demo.presupuesto.api.TransferredNeedMonth;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class BudgetNeedsTransferAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private BudgetNeedsTransferAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:budget-transfer-" + System.nanoTime()
                        + ";DB_CLOSE_DELAY=-1;NON_KEYWORDS=MONTH",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema();
        seedReferences();
        adapter = new BudgetNeedsTransferAdapter(jdbcTemplate, new JdbcIdempotencyAdapter(jdbcTemplate));
    }

    @Test
    void shouldCreateUnitBudgetFromNeedsTransferAndReplayIdempotently() {
        TransferNeedsCommand command = command(new IdempotencyKey("budget-transfer-001"));

        BudgetTransferResult first = adapter.transfer(command);
        BudgetTransferResult replay = adapter.transfer(command);

        assertNotNull(first.transferId());
        assertNotNull(first.unitBudgetExerciseId());
        assertEquals(1, first.transferredLines());
        assertFalse(first.replayed());
        assertEquals(first.transferId(), replay.transferId());
        assertEquals(first.unitBudgetExerciseId(), replay.unitBudgetExerciseId());
        assertTrue(replay.replayed());

        assertEquals(1, count("presupuesto.budget_exercises"));
        assertEquals(1, count("presupuesto.budget_transfers"));
        assertEquals(2, count("presupuesto.budget_lines"));
        assertEquals(2, count("presupuesto.budget_movements"));
        assertEquals(2, count("presupuesto.budget_transfer_lines"));
        assertDecimalEquals("300.00", jdbcTemplate.queryForObject(
                "SELECT SUM(assigned_amount) FROM presupuesto.budget_lines",
                BigDecimal.class));
        assertDecimalEquals("300.00", jdbcTemplate.queryForObject(
                "SELECT SUM(amount) FROM presupuesto.budget_movements",
                BigDecimal.class));
    }

    @Test
    void shouldReturnExistingTransferWithoutDuplicatingWhenConsolidationWasAlreadyReceived() {
        BudgetTransferResult first = adapter.transfer(command(new IdempotencyKey("budget-transfer-002")));
        BudgetTransferResult repeatedConsolidation = adapter.transfer(command(new IdempotencyKey("budget-transfer-003")));

        assertEquals(first.transferId(), repeatedConsolidation.transferId());
        assertTrue(repeatedConsolidation.replayed());
        assertEquals(1, count("presupuesto.budget_transfers"));
        assertEquals(2, count("presupuesto.budget_movements"));
    }

    private TransferNeedsCommand command(IdempotencyKey idempotencyKey) {
        return new TransferNeedsCommand(
                900L,
                1L,
                2026,
                List.of(new TransferredNeedLine(
                        100L,
                        200L,
                        300L,
                        List.of(
                                month(1, "120.00"),
                                month(2, "180.00")))),
                idempotencyKey,
                "aprobador");
    }

    private TransferredNeedMonth month(int month, String amount) {
        return new TransferredNeedMonth(
                new FiscalDimension(1L, 2026, month, 10L, 20L, 30L, 40L),
                new BigDecimal("1.0000"),
                new Money(new BigDecimal(amount), Moneda.PEN));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private void seedReferences() {
        jdbcTemplate.update("INSERT INTO platform.companies(id) VALUES (1)");
        jdbcTemplate.update("INSERT INTO platform.currencies(code) VALUES ('PEN')");
        jdbcTemplate.update("INSERT INTO platform.cost_centers(id) VALUES (10)");
        jdbcTemplate.update("INSERT INTO platform.financing_sources(id) VALUES (20)");
        jdbcTemplate.update("INSERT INTO platform.goals(id) VALUES (30)");
        jdbcTemplate.update("INSERT INTO platform.expense_classifiers(id) VALUES (40)");
        jdbcTemplate.update("INSERT INTO platform.catalog_items(id) VALUES (300)");
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE SCHEMA platform");
        jdbcTemplate.execute("CREATE SCHEMA presupuesto");
        jdbcTemplate.execute("CREATE TABLE platform.companies(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.currencies(code CHAR(3) PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.cost_centers(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.financing_sources(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.goals(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.expense_classifiers(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.catalog_items(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("""
                CREATE TABLE platform.idempotency_keys(
                    id UUID PRIMARY KEY,
                    scope VARCHAR(80) NOT NULL,
                    idempotency_key VARCHAR(120) NOT NULL,
                    request_hash VARCHAR(128) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    response_status INTEGER,
                    response_content_type VARCHAR(100),
                    response_body CLOB,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    completed_at TIMESTAMP WITH TIME ZONE,
                    UNIQUE(scope, idempotency_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_exercises(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    fiscal_year SMALLINT NOT NULL,
                    exercise_type VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    approved_at TIMESTAMP WITH TIME ZONE,
                    created_by VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_by VARCHAR(100) NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE(company_id, fiscal_year, exercise_type)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_lines(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    budget_exercise_id BIGINT NOT NULL,
                    company_id BIGINT NOT NULL,
                    fiscal_year SMALLINT NOT NULL,
                    month SMALLINT NOT NULL,
                    cost_center_id BIGINT NOT NULL,
                    financing_source_id BIGINT NOT NULL,
                    goal_id BIGINT NOT NULL,
                    expense_classifier_id BIGINT NOT NULL,
                    currency_code CHAR(3) NOT NULL,
                    assigned_amount NUMERIC(18,2) NOT NULL,
                    precommitted_amount NUMERIC(18,2) NOT NULL,
                    committed_amount NUMERIC(18,2) NOT NULL,
                    created_by VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_by VARCHAR(100) NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE(budget_exercise_id, month, cost_center_id, financing_source_id, goal_id, expense_classifier_id, currency_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_movements(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    budget_line_id BIGINT NOT NULL,
                    movement_type VARCHAR(30) NOT NULL,
                    source_module VARCHAR(40) NOT NULL,
                    source_type VARCHAR(40) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_number VARCHAR(80) NOT NULL,
                    amount NUMERIC(18,2) NOT NULL,
                    currency_code CHAR(3) NOT NULL,
                    actor VARCHAR(100) NOT NULL,
                    registered_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_transfers(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    consolidation_id BIGINT NOT NULL UNIQUE,
                    company_id BIGINT NOT NULL,
                    fiscal_year SMALLINT NOT NULL,
                    unit_budget_exercise_id BIGINT NOT NULL,
                    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
                    transferred_lines INTEGER NOT NULL,
                    actor VARCHAR(100) NOT NULL,
                    transferred_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    created_by VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_by VARCHAR(100) NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_transfer_lines(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    transfer_id BIGINT NOT NULL,
                    budget_line_id BIGINT NOT NULL,
                    needs_plan_id BIGINT NOT NULL,
                    needs_line_id BIGINT NOT NULL,
                    catalog_item_id BIGINT NOT NULL,
                    month SMALLINT NOT NULL,
                    approved_quantity NUMERIC(18,4) NOT NULL,
                    amount NUMERIC(18,2) NOT NULL,
                    UNIQUE(transfer_id, needs_line_id, month)
                )
                """);
    }
}
