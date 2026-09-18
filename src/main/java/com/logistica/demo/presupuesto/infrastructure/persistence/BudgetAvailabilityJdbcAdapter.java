package com.logistica.demo.presupuesto.infrastructure.persistence;

import com.logistica.demo.presupuesto.api.BudgetAvailability;
import com.logistica.demo.presupuesto.api.BudgetAvailabilityQuery;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BudgetAvailabilityJdbcAdapter implements BudgetAvailabilityQuery {

    private final JdbcTemplate jdbcTemplate;

    public BudgetAvailabilityJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<BudgetAvailability> findAvailability(FiscalDimension dimension, Moneda currency) {
        return findState(dimension, currency, false).map(BudgetLineState::toAvailability);
    }

    Optional<BudgetLineState> findStateForUpdate(FiscalDimension dimension, Moneda currency) {
        return findState(dimension, currency, true);
    }

    private Optional<BudgetLineState> findState(FiscalDimension dimension, Moneda currency, boolean forUpdate) {
        String sql = """
                SELECT line.id,
                       line.company_id,
                       line.fiscal_year,
                       line.month,
                       line.cost_center_id,
                       line.financing_source_id,
                       line.goal_id,
                       line.expense_classifier_id,
                       line.currency_code,
                       line.assigned_amount,
                       line.precommitted_amount,
                       line.committed_amount
                FROM presupuesto.budget_lines line
                JOIN presupuesto.budget_exercises exercise
                  ON exercise.id = line.budget_exercise_id
                WHERE exercise.company_id = ?
                  AND exercise.fiscal_year = ?
                  AND exercise.exercise_type = 'PIM'
                  AND exercise.status = 'APPROVED'
                  AND line.month = ?
                  AND line.cost_center_id = ?
                  AND line.financing_source_id = ?
                  AND line.goal_id = ?
                  AND line.expense_classifier_id = ?
                  AND line.currency_code = ?
                """ + (forUpdate ? " FOR UPDATE" : "");
        List<BudgetLineState> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new BudgetLineState(
                        rs.getLong("id"),
                        new FiscalDimension(
                                rs.getLong("company_id"),
                                rs.getInt("fiscal_year"),
                                rs.getInt("month"),
                                rs.getLong("cost_center_id"),
                                rs.getLong("financing_source_id"),
                                rs.getLong("goal_id"),
                                rs.getLong("expense_classifier_id")),
                        Moneda.valueOf(rs.getString("currency_code").trim()),
                        rs.getBigDecimal("assigned_amount"),
                        rs.getBigDecimal("precommitted_amount"),
                        rs.getBigDecimal("committed_amount")),
                dimension.companyId(),
                dimension.year(),
                dimension.month(),
                dimension.costCenterId(),
                dimension.financingSourceId(),
                dimension.goalId(),
                dimension.expenseClassifierId(),
                currency.name());
        return rows.stream().findFirst();
    }
}
