package com.logistica.demo.cuadronecesidades.infrastructure.persistence;

import com.logistica.demo.cuadronecesidades.api.MonthlyNeedBalance;
import com.logistica.demo.cuadronecesidades.api.NeedsBalanceQuery;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcNeedsBalanceQuery implements NeedsBalanceQuery {

    private final JdbcTemplate jdbcTemplate;

    public JdbcNeedsBalanceQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<NeedsLineBalance> findAvailableLine(Long companyId, Long needsLineId) {
        return jdbcTemplate.query(
                        """
                        SELECT
                            p.id AS needs_plan_id,
                            l.id AS needs_line_id,
                            p.company_id,
                            p.fiscal_year,
                            p.cost_center_id,
                            p.financing_source_id,
                            p.goal_id,
                            l.expense_classifier_id,
                            l.catalog_item_id,
                            l.approved_quantity,
                            l.consumed_quantity,
                            l.approved_quantity - l.consumed_quantity AS available_quantity
                        FROM cuadronecesidades.need_lines l
                        JOIN cuadronecesidades.needs_plans p ON p.id = l.needs_plan_id
                        WHERE p.company_id = ?
                            AND l.id = ?
                            AND p.status = 'TRANSFERRED'
                            AND l.approved_quantity IS NOT NULL
                        """,
                        (rs, rowNumber) -> mapLine(rs, readMonths(needsLineId)),
                        companyId,
                        needsLineId)
                .stream()
                .findFirst();
    }

    private NeedsLineBalance mapLine(ResultSet rs, List<MonthlyNeedBalance> months) throws SQLException {
        return new NeedsLineBalance(
                rs.getLong("needs_plan_id"),
                rs.getLong("needs_line_id"),
                rs.getLong("company_id"),
                rs.getInt("fiscal_year"),
                rs.getLong("cost_center_id"),
                rs.getLong("financing_source_id"),
                rs.getLong("goal_id"),
                rs.getLong("expense_classifier_id"),
                rs.getLong("catalog_item_id"),
                rs.getBigDecimal("approved_quantity"),
                rs.getBigDecimal("consumed_quantity"),
                rs.getBigDecimal("available_quantity"),
                months);
    }

    private List<MonthlyNeedBalance> readMonths(Long needsLineId) {
        return jdbcTemplate.query(
                """
                SELECT
                    month,
                    approved_quantity,
                    consumed_quantity,
                    approved_quantity - consumed_quantity AS available_quantity
                FROM cuadronecesidades.monthly_needs
                WHERE need_line_id = ?
                    AND approved_quantity IS NOT NULL
                ORDER BY month
                """,
                (rs, rowNumber) -> new MonthlyNeedBalance(
                        rs.getInt("month"),
                        value(rs, "approved_quantity"),
                        value(rs, "consumed_quantity"),
                        value(rs, "available_quantity")),
                needsLineId);
    }

    private BigDecimal value(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column);
    }
}
