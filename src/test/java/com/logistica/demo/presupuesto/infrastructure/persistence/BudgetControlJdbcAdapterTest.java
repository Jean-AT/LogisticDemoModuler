package com.logistica.demo.presupuesto.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.demo.platform.infrastructure.persistence.JdbcIdempotencyAdapter;
import com.logistica.demo.presupuesto.api.BudgetAllocation;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlStatus;
import com.logistica.demo.presupuesto.api.CommitBudgetCommand;
import com.logistica.demo.presupuesto.api.PrecommitBudgetCommand;
import com.logistica.demo.presupuesto.api.ReleaseBudgetCommand;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class BudgetControlJdbcAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private BudgetControlJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:budget-control-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;NON_KEYWORDS=MONTH",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema();
        seedPim();
        BudgetAvailabilityJdbcAdapter availability = new BudgetAvailabilityJdbcAdapter(jdbcTemplate);
        adapter = new BudgetControlJdbcAdapter(jdbcTemplate, availability, new JdbcIdempotencyAdapter(jdbcTemplate));
    }

    @Test
    void shouldPrecommitBudgetIdempotentlyAndPreventDuplicatedActivePrecommit() {
        PrecommitBudgetCommand command = precommit("REQ-001", "precommit-001", "250.00");

        BudgetControlResult first = adapter.precommit(command);
        BudgetControlResult replay = adapter.precommit(command);

        assertEquals(first.budgetControlId(), replay.budgetControlId());
        assertEquals(BudgetControlStatus.PRECOMMITTED, replay.status());
        assertDecimalEquals("250.00", precommittedAmount());
        assertDecimalEquals("750.00", replay.availableAfter().amount());
        assertEquals(1, count("presupuesto.budget_controls"));
        assertEquals(1, count("presupuesto.budget_movements"));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> adapter.precommit(precommit("REQ-001", "precommit-002", "50.00")));

        assertEquals("Existe un precompromiso activo para el documento origen.", exception.getMessage());
    }

    @Test
    void shouldCommitAndReleaseBudgetWithoutExceedingSourceBalance() {
        BudgetControlResult precommit = adapter.precommit(precommit("REQ-002", "precommit-003", "300.00"));
        BudgetControlResult committed = adapter.commit(new CommitBudgetCommand(
                precommit.budgetControlId(),
                source("REQ-002"),
                List.of(allocation("180.00")),
                new IdempotencyKey("commit-001"),
                "buyer"));
        BudgetControlResult released = adapter.release(new ReleaseBudgetCommand(
                precommit.budgetControlId(),
                source("REQ-002"),
                List.of(allocation("120.00")),
                "diferencia adjudicada",
                new IdempotencyKey("release-001"),
                "buyer"));

        assertEquals(BudgetControlStatus.COMMITTED, committed.status());
        assertEquals(BudgetControlStatus.COMMITTED, released.status());
        assertDecimalEquals("0.00", precommittedAmount());
        assertDecimalEquals("180.00", committedAmount());
        assertDecimalEquals("820.00", released.availableAfter().amount());

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> adapter.release(new ReleaseBudgetCommand(
                        precommit.budgetControlId(),
                        source("REQ-002"),
                        List.of(allocation("181.00")),
                        "exceso",
                        new IdempotencyKey("release-002"),
                        "buyer")));

        assertEquals("La liberacion supera el saldo del documento origen.", exception.getMessage());
        assertTrue(count("presupuesto.budget_movements") >= 4);
    }

    private PrecommitBudgetCommand precommit(String number, String idempotencyKey, String amount) {
        return new PrecommitBudgetCommand(
                source(number),
                List.of(allocation(amount)),
                new IdempotencyKey(idempotencyKey),
                "buyer");
    }

    private DocumentReference source(String number) {
        return new DocumentReference("LOGISTICA", "REQUERIMIENTO", 500L, number);
    }

    private BudgetAllocation allocation(String amount) {
        return new BudgetAllocation(
                new FiscalDimension(1L, 2026, 1, 10L, 20L, 30L, 40L),
                new Money(new BigDecimal(amount), Moneda.PEN));
    }

    private BigDecimal precommittedAmount() {
        return jdbcTemplate.queryForObject(
                "SELECT precommitted_amount FROM presupuesto.budget_lines WHERE id = 10",
                BigDecimal.class);
    }

    private BigDecimal committedAmount() {
        return jdbcTemplate.queryForObject(
                "SELECT committed_amount FROM presupuesto.budget_lines WHERE id = 10",
                BigDecimal.class);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private void seedPim() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_exercises (
                    id, company_id, fiscal_year, exercise_type, status, approved_at,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (1, 1, 2026, 'PIM', 'APPROVED', ?, 'system', ?, 'system', ?)
                """,
                now,
                now,
                now);
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_lines (
                    id, budget_exercise_id, company_id, fiscal_year, month,
                    cost_center_id, financing_source_id, goal_id, expense_classifier_id,
                    currency_code, assigned_amount, precommitted_amount, committed_amount,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (10, 1, 1, 2026, 1, 10, 20, 30, 40, 'PEN', 1000, 0, 0, 'system', ?, 'system', ?)
                """,
                now,
                now);
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE SCHEMA platform");
        jdbcTemplate.execute("CREATE SCHEMA presupuesto");
        jdbcTemplate.execute("CREATE TABLE platform.currencies(code CHAR(3) PRIMARY KEY)");
        jdbcTemplate.execute("INSERT INTO platform.currencies(code) VALUES ('PEN')");
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
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
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
                    version BIGINT NOT NULL DEFAULT 0,
                    created_by VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_by VARCHAR(100) NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
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
                    idempotency_key VARCHAR(120),
                    actor VARCHAR(100) NOT NULL,
                    registered_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_controls(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    source_module VARCHAR(40) NOT NULL,
                    source_type VARCHAR(40) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_number VARCHAR(80) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    actor VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_control_lines(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    budget_control_id BIGINT NOT NULL,
                    budget_line_id BIGINT NOT NULL,
                    amount NUMERIC(18,2) NOT NULL,
                    currency_code CHAR(3) NOT NULL,
                    precommitted_amount NUMERIC(18,2) NOT NULL,
                    committed_amount NUMERIC(18,2) NOT NULL,
                    released_amount NUMERIC(18,2) NOT NULL,
                    UNIQUE(budget_control_id, budget_line_id)
                )
                """);
    }
}
