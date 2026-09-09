package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.MasterDataReference;
import com.logistica.demo.platform.api.PlatformCatalogQuery;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPlatformCatalogQuery implements PlatformCatalogQuery {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPlatformCatalogQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MasterDataReference> findActiveCompany(Long companyId) {
        return findOne(
                """
                SELECT id, code, legal_name AS name, active
                FROM platform.companies
                WHERE id = ? AND active = TRUE
                """,
                companyId);
    }

    @Override
    public Optional<MasterDataReference> findActiveCostCenter(Long companyId, Long costCenterId) {
        return findOne(
                """
                SELECT id, code, name, active
                FROM platform.cost_centers
                WHERE company_id = ?
                  AND id = ?
                  AND active = TRUE
                  AND (valid_from IS NULL OR valid_from <= CURRENT_DATE)
                  AND (valid_to IS NULL OR valid_to >= CURRENT_DATE)
                """,
                companyId,
                costCenterId);
    }

    @Override
    public Optional<MasterDataReference> findActiveFinancingSource(Long financingSourceId) {
        return findOne(
                """
                SELECT id, code, name, active
                FROM platform.financing_sources
                WHERE id = ? AND active = TRUE
                """,
                financingSourceId);
    }

    @Override
    public Optional<MasterDataReference> findActiveGoal(Long goalId) {
        return findOne(
                """
                SELECT id, code, name, active
                FROM platform.goals
                WHERE id = ? AND active = TRUE
                """,
                goalId);
    }

    @Override
    public Optional<MasterDataReference> findActiveExpenseClassifier(Long expenseClassifierId) {
        return findOne(
                """
                SELECT id, code, name, active
                FROM platform.expense_classifiers
                WHERE id = ? AND active = TRUE
                """,
                expenseClassifierId);
    }

    @Override
    public Optional<MasterDataReference> findActiveCatalogItem(Long catalogItemId) {
        return findOne(
                """
                SELECT id, code, name, active
                FROM platform.catalog_items
                WHERE id = ? AND active = TRUE
                """,
                catalogItemId);
    }

    private Optional<MasterDataReference> findOne(String sql, Object... parameters) {
        return jdbcTemplate.query(
                        sql,
                        (resultSet, rowNumber) -> new MasterDataReference(
                                resultSet.getLong("id"),
                                resultSet.getString("code"),
                                resultSet.getString("name"),
                                resultSet.getBoolean("active")),
                        parameters)
                .stream()
                .findFirst();
    }
}
