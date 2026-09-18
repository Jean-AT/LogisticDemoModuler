package com.logistica.demo.presupuesto.infrastructure.persistence;

import com.logistica.demo.presupuesto.api.ApproveBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.BudgetPlanResult;
import com.logistica.demo.presupuesto.api.BudgetPlanUseCase;
import com.logistica.demo.presupuesto.api.GenerateBudgetPlanCommand;
import com.logistica.demo.presupuesto.api.ReviewBudgetPlanCommand;
import com.logistica.demo.shared.exception.BusinessRuleException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetPlanJdbcAdapter implements BudgetPlanUseCase {

    private final JdbcTemplate jdbcTemplate;

    public BudgetPlanJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public BudgetPlanResult generatePia(GenerateBudgetPlanCommand command) {
        long unitExerciseId = requiredExercise(command.companyId(), command.fiscalYear(), "UNIDADES", "APPROVED");
        int unitLines = countLines(unitExerciseId);
        if (unitLines == 0) {
            throw new BusinessRuleException("No existen lineas de Unidades para generar el PIA.");
        }

        Long existingPia = findExercise(command.companyId(), command.fiscalYear(), "PIA");
        if (existingPia != null) {
            return new BudgetPlanResult(existingPia, findExercise(command.companyId(), command.fiscalYear(), "PIM"), countLines(existingPia), true);
        }

        Instant now = Instant.now();
        createExercise(command.companyId(), command.fiscalYear(), "PIA", "DRAFT", command.actor(), now);
        long piaExerciseId = requiredExercise(command.companyId(), command.fiscalYear(), "PIA", "DRAFT");
        copyLines(unitExerciseId, piaExerciseId, command.actor(), now);
        registerAssignments(piaExerciseId, "PIA_GENERATION", unitExerciseId, "PIA-" + command.fiscalYear(), command.actor(), now);
        return new BudgetPlanResult(piaExerciseId, null, countLines(piaExerciseId), false);
    }

    @Override
    @Transactional
    public BudgetPlanResult reviewPia(ReviewBudgetPlanCommand command) {
        long piaExerciseId = requiredExercise(command.companyId(), command.fiscalYear(), "PIA", "DRAFT");
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_plan_reviews (
                    pia_exercise_id, reviewer, reviewed_at, notes
                )
                SELECT ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM presupuesto.budget_plan_reviews
                    WHERE pia_exercise_id = ?
                )
                """,
                piaExerciseId,
                command.reviewer(),
                Instant.now(),
                command.notes(),
                piaExerciseId);
        return new BudgetPlanResult(piaExerciseId, findExercise(command.companyId(), command.fiscalYear(), "PIM"), countLines(piaExerciseId), true);
    }

    @Override
    @Transactional
    public BudgetPlanResult approvePiaAndCreateInitialPim(ApproveBudgetPlanCommand command) {
        Long piaExerciseId = findExercise(command.companyId(), command.fiscalYear(), "PIA");
        if (piaExerciseId == null) {
            throw new BusinessRuleException("Debe generar el PIA antes de aprobarlo.");
        }
        if (!hasReview(piaExerciseId)) {
            throw new BusinessRuleException("Debe revisar el PIA antes de aprobarlo.");
        }
        Long existingPim = findExercise(command.companyId(), command.fiscalYear(), "PIM");
        if (isApproved(piaExerciseId) && existingPim != null) {
            return new BudgetPlanResult(piaExerciseId, existingPim, countLines(existingPim), true);
        }

        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                UPDATE presupuesto.budget_exercises
                SET status = 'APPROVED',
                    approved_at = ?,
                    updated_by = ?,
                    updated_at = ?,
                    version = version + 1
                WHERE id = ?
                  AND status = 'DRAFT'
                """,
                now,
                command.actor(),
                now,
                piaExerciseId);

        if (existingPim == null) {
            createExercise(command.companyId(), command.fiscalYear(), "PIM", "APPROVED", command.actor(), now);
            existingPim = requiredExercise(command.companyId(), command.fiscalYear(), "PIM", "APPROVED");
            copyLines(piaExerciseId, existingPim, command.actor(), now);
            registerAssignments(existingPim, "PIM_INITIAL", piaExerciseId, "PIM-" + command.fiscalYear(), command.actor(), now);
        }

        return new BudgetPlanResult(piaExerciseId, existingPim, countLines(existingPim), false);
    }

    private void createExercise(Long companyId, int fiscalYear, String type, String status, String actor, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_exercises (
                    company_id, fiscal_year, exercise_type, status, approved_at,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                companyId,
                fiscalYear,
                type,
                status,
                "APPROVED".equals(status) ? now : null,
                actor,
                now,
                actor,
                now);
    }

    private void copyLines(long sourceExerciseId, long targetExerciseId, String actor, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_lines (
                    budget_exercise_id, company_id, fiscal_year, month,
                    cost_center_id, financing_source_id, goal_id, expense_classifier_id,
                    currency_code, assigned_amount, precommitted_amount, committed_amount,
                    created_by, created_at, updated_by, updated_at
                )
                SELECT ?, company_id, fiscal_year, month,
                       cost_center_id, financing_source_id, goal_id, expense_classifier_id,
                       currency_code, assigned_amount, 0, 0,
                       ?, ?, ?, ?
                FROM presupuesto.budget_lines source
                WHERE source.budget_exercise_id = ?
                  AND source.assigned_amount > 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM presupuesto.budget_lines target
                      WHERE target.budget_exercise_id = ?
                        AND target.month = source.month
                        AND target.cost_center_id = source.cost_center_id
                        AND target.financing_source_id = source.financing_source_id
                        AND target.goal_id = source.goal_id
                        AND target.expense_classifier_id = source.expense_classifier_id
                        AND target.currency_code = source.currency_code
                  )
                """,
                targetExerciseId,
                actor,
                now,
                actor,
                now,
                sourceExerciseId,
                targetExerciseId);
    }

    private void registerAssignments(
            long exerciseId,
            String sourceType,
            long sourceId,
            String sourceNumber,
            String actor,
            Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO presupuesto.budget_movements (
                    budget_line_id, movement_type, source_module, source_type, source_id,
                    source_number, amount, currency_code, actor, registered_at
                )
                SELECT line.id, 'ASSIGNMENT', 'PRESUPUESTO', ?, ?, ?,
                       line.assigned_amount, line.currency_code, ?, ?
                FROM presupuesto.budget_lines line
                WHERE line.budget_exercise_id = ?
                  AND line.assigned_amount > 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM presupuesto.budget_movements movement
                      WHERE movement.budget_line_id = line.id
                        AND movement.movement_type = 'ASSIGNMENT'
                        AND movement.source_module = 'PRESUPUESTO'
                        AND movement.source_type = ?
                        AND movement.source_id = ?
                  )
                """,
                sourceType,
                sourceId,
                sourceNumber,
                actor,
                now,
                exerciseId,
                sourceType,
                sourceId);
    }

    private long requiredExercise(Long companyId, int fiscalYear, String type, String status) {
        List<Long> rows = jdbcTemplate.query(
                """
                SELECT id
                FROM presupuesto.budget_exercises
                WHERE company_id = ?
                  AND fiscal_year = ?
                  AND exercise_type = ?
                  AND status = ?
                """,
                (rs, rowNum) -> rs.getLong("id"),
                companyId,
                fiscalYear,
                type,
                status);
        if (rows.isEmpty()) {
            throw new BusinessRuleException("No existe un ejercicio " + type + " en estado " + status + ".");
        }
        return rows.get(0);
    }

    private Long findExercise(Long companyId, int fiscalYear, String type) {
        List<Long> rows = jdbcTemplate.query(
                """
                SELECT id
                FROM presupuesto.budget_exercises
                WHERE company_id = ?
                  AND fiscal_year = ?
                  AND exercise_type = ?
                """,
                (rs, rowNum) -> rs.getLong("id"),
                companyId,
                fiscalYear,
                type);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean isApproved(long exerciseId) {
        Boolean approved = jdbcTemplate.queryForObject(
                "SELECT status = 'APPROVED' FROM presupuesto.budget_exercises WHERE id = ?",
                Boolean.class,
                exerciseId);
        return Boolean.TRUE.equals(approved);
    }

    private boolean hasReview(long piaExerciseId) {
        Integer reviews = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM presupuesto.budget_plan_reviews WHERE pia_exercise_id = ?",
                Integer.class,
                piaExerciseId);
        return reviews != null && reviews > 0;
    }

    private int countLines(long exerciseId) {
        Integer lines = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM presupuesto.budget_lines WHERE budget_exercise_id = ?",
                Integer.class,
                exerciseId);
        return lines == null ? 0 : lines;
    }
}
