package com.logistica.demo.presupuesto.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.demo.presupuesto.api.ApproveBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.BudgetPlanResult;
import com.logistica.demo.presupuesto.api.GenerateBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.ReviewBudgetPlanCommand;
import com.logistica.demo.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class BudgetPlanJdbcAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private BudgetPlanJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:budget-plan-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;NON_KEYWORDS=MONTH",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema();
        seedUnitBudget();
        adapter = new BudgetPlanJdbcAdapter(jdbcTemplate);
    }

    @Test
    void shouldGenerateReviewApprovePiaAndCreateInitialPim() {
        BudgetPlanResult generated = adapter.generatePia(new GenerateBudgetPlanCommand(1L, 2026, "planner"));
        BudgetPlanResult reviewed = adapter.reviewPia(new ReviewBudgetPlanCommand(1L, 2026, "reviewer", "ok"));
        BudgetPlanResult approved = adapter.approvePiaAndCreateInitialPim(new ApproveBudgetPlanCommand(1L, 2026, "approver"));

        assertNotNull(generated.piaExerciseId());
        assertEquals(generated.piaExerciseId(), reviewed.piaExerciseId());
        assertEquals(generated.piaExerciseId(), approved.piaExerciseId());
        assertNotNull(approved.pimExerciseId());
        assertEquals(2, approved.lines());
        assertEquals("APPROVED", status(generated.piaExerciseId()));
        assertEquals("APPROVED", status(approved.pimExerciseId()));
        assertEquals(1, count("presupuesto.budget_plan_reviews"));
        assertEquals(3, count("presupuesto.budget_exercises"));
        assertEquals(6, count("presupuesto.budget_lines"));
        assertEquals(4, count("presupuesto.budget_movements"));
        assertDecimalEquals("300.00", assigned(generated.piaExerciseId()));
        assertDecimalEquals("300.00", assigned(approved.pimExerciseId()));
    }

    @Test
    void shouldRequireReviewBeforeApprovingPia() {
        adapter.generatePia(new GenerateBudgetPlanCommand(1L, 2026, "planner"));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> adapter.approvePiaAndCreateInitialPim(new ApproveBudgetPlanCommand(1L, 2026, "approver")));

        assertEquals("Debe revisar el PIA antes de aprobarlo.", exception.getMessage());
    }

    @Test
    void shouldReplayExistingPiaAndPimWithoutDuplicatingLines() {
        BudgetPlanResult firstPia = adapter.generatePia(new GenerateBudgetPlanCommand(1L, 2026, "planner"));
        BudgetPlanResult repeatedPia = adapter.generatePia(new GenerateBudgetPlanCommand(1L, 2026, "planner"));
        adapter.reviewPia(new ReviewBudgetPlanCommand(1L, 2026, "reviewer", null));
        BudgetPlanResult firstApproval = adapter.approvePiaAndCreateInitialPim(new ApproveBudgetPlanCommand(1L, 2026, "approver"));
        BudgetPlanResult repeatedApproval = adapter.approvePiaAndCreateInitialPim(new ApproveBudgetPlanCommand(1L, 2026, "approver"));

        assertTrue(repeatedPia.replayed());
        assertTrue(repeatedApproval.replayed());
        assertEquals(firstPia.piaExerciseId(), repeatedPia.piaExerciseId());
        assertEquals(firstApproval.pimExerciseId(), repeatedApproval.pimExerciseId());
        assertEquals(3, count("presupuesto.budget_exercises"));
        assertEquals(6, count("presupuesto.budget_lines"));
        assertEquals(4, count("presupuesto.budget_movements"));
    }

    private String status(Long exerciseId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM presupuesto.budget_exercises WHERE id = ?",
                String.class,
                exerciseId);
    }

    private BigDecimal assigned(Long exerciseId) {
        return jdbcTemplate.queryForObject(
                "SELECT SUM(assigned_amount) FROM presupuesto.budget_lines WHERE budget_exercise_id = ?",
                BigDecimal.class,
                exerciseId);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private void seedUnitBudget() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_exercises (
                    id, company_id, fiscal_year, exercise_type, status, approved_at,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (100, 1, 2026, 'UNIDADES', 'APPROVED', ?, 'system', ?, 'system', ?)
                """,
                now,
                now,
                now);
        insertUnitLine(1000L, 1, "120.00");
        insertUnitLine(1001L, 2, "180.00");
    }

    private void insertUnitLine(Long id, int month, String amount) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_lines (
                    id, budget_exercise_id, company_id, fiscal_year, month,
                    cost_center_id, financing_source_id, goal_id, expense_classifier_id,
                    currency_code, assigned_amount, precommitted_amount, committed_amount,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (?, 100, 1, 2026, ?, 10, 20, 30, 40, 'PEN', ?, 0, 0, 'system', ?, 'system', ?)
                """,
                id,
                month,
                new BigDecimal(amount),
                now,
                now);
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE SCHEMA platform");
        jdbcTemplate.execute("CREATE SCHEMA presupuesto");
        jdbcTemplate.execute("CREATE TABLE platform.companies(id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE platform.currencies(code CHAR(3) PRIMARY KEY)");
        jdbcTemplate.execute("INSERT INTO platform.companies(id) VALUES (1)");
        jdbcTemplate.execute("INSERT INTO platform.currencies(code) VALUES ('PEN')");
        jdbcTemplate.execute("""
                CREATE TABLE presupuesto.budget_exercises(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    fiscal_year SMALLINT NOT NULL,
                    exercise_type VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    approved_at TIMESTAMP WITH TIME ZONE,
                    closed_at TIMESTAMP WITH TIME ZONE,
                    version BIGINT NOT NULL DEFAULT 0,
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
                CREATE TABLE presupuesto.budget_plan_reviews(
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    pia_exercise_id BIGINT NOT NULL UNIQUE,
                    reviewer VARCHAR(100) NOT NULL,
                    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    notes VARCHAR(500)
                )
                """);
    }
}
