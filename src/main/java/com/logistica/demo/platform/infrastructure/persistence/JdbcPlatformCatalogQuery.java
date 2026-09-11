package com.logistica.demo.platform.infrastructure.persistence;

import com.logistica.demo.platform.api.CatalogItemReference;
import com.logistica.demo.platform.api.CurrencyReference;
import com.logistica.demo.platform.api.MasterDataReference;
import com.logistica.demo.platform.api.PlatformCatalogQuery;
import com.logistica.demo.platform.api.UnitOfMeasureReference;
import java.util.List;
import java.util.Locale;
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

    @Override
    public Optional<CurrencyReference> findActiveCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                        """
                        SELECT code, name, symbol, decimal_places, active
                        FROM platform.currencies
                        WHERE code = ? AND active = TRUE
                        """,
                        (rs, rowNumber) -> new CurrencyReference(
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getString("symbol"),
                                rs.getInt("decimal_places"),
                                rs.getBoolean("active")),
                        currencyCode.trim().toUpperCase(Locale.ROOT))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UnitOfMeasureReference> findActiveUnitOfMeasure(String unitCode) {
        if (unitCode == null || unitCode.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                        """
                        SELECT id, code, name, active
                        FROM platform.units_of_measure
                        WHERE code = ? AND active = TRUE
                        """,
                        (rs, rowNumber) -> new UnitOfMeasureReference(
                                rs.getLong("id"),
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getBoolean("active")),
                        unitCode.trim().toUpperCase(Locale.ROOT))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<CatalogItemReference> findActiveCatalogItemByCode(Long companyId, String itemCode) {
        if (companyId == null || companyId <= 0 || itemCode == null || itemCode.isBlank()) {
            return Optional.empty();
        }
        return queryCatalogItems(
                        """
                        WHERE ci.company_id = ?
                          AND ci.code = ?
                          AND ci.active = TRUE
                        """,
                        companyId,
                        itemCode.trim().toUpperCase(Locale.ROOT))
                .stream()
                .findFirst();
    }

    @Override
    public List<CatalogItemReference> findActiveCatalogItems(Long companyId) {
        if (companyId == null || companyId <= 0) {
            return List.of();
        }
        return queryCatalogItems(
                """
                WHERE ci.company_id = ?
                  AND ci.active = TRUE
                ORDER BY ci.code
                """,
                companyId);
    }

    private List<CatalogItemReference> queryCatalogItems(String whereClause, Object... parameters) {
        return jdbcTemplate.query(
                """
                SELECT ci.id,
                       ci.company_id,
                       ci.code,
                       ci.name,
                       ci.item_type,
                       ci.active,
                       uom.id AS unit_id,
                       uom.code AS unit_code,
                       uom.name AS unit_name,
                       uom.active AS unit_active,
                       ec.id AS classifier_id,
                       ec.code AS classifier_code,
                       ec.name AS classifier_name,
                       ec.active AS classifier_active
                FROM platform.catalog_items ci
                JOIN platform.units_of_measure uom ON uom.id = ci.unit_of_measure_id
                JOIN platform.expense_classifiers ec
                  ON ec.company_id = ci.company_id
                 AND ec.id = ci.expense_classifier_id
                %s
                """.formatted(whereClause),
                (rs, rowNumber) -> new CatalogItemReference(
                        rs.getLong("id"),
                        rs.getLong("company_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("item_type"),
                        new UnitOfMeasureReference(
                                rs.getLong("unit_id"),
                                rs.getString("unit_code"),
                                rs.getString("unit_name"),
                                rs.getBoolean("unit_active")),
                        new MasterDataReference(
                                rs.getLong("classifier_id"),
                                rs.getString("classifier_code"),
                                rs.getString("classifier_name"),
                                rs.getBoolean("classifier_active")),
                        rs.getBoolean("active")),
                parameters);
    }
}
