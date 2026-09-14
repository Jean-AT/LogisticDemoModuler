package com.logistica.demo.platform.api;

import java.util.List;
import java.util.Optional;

public interface PlatformCatalogQuery {

    Optional<MasterDataReference> findActiveCompany(Long companyId);

    java.util.List<MasterDataReference> findActiveCompanies();

    Optional<MasterDataReference> findActiveCostCenter(Long companyId, Long costCenterId);

    java.util.List<MasterDataReference> findActiveCostCenters(Long companyId);

    Optional<MasterDataReference> findActiveFinancingSource(Long financingSourceId);

    java.util.List<MasterDataReference> findActiveFinancingSources(Long companyId);

    Optional<MasterDataReference> findActiveGoal(Long goalId);

    java.util.List<MasterDataReference> findActiveGoals(Long companyId, int fiscalYear);

    Optional<MasterDataReference> findActiveExpenseClassifier(Long expenseClassifierId);

    java.util.List<MasterDataReference> findActiveExpenseClassifiers(Long companyId);

    Optional<MasterDataReference> findActiveCatalogItem(Long catalogItemId);

    Optional<CurrencyReference> findActiveCurrency(String currencyCode);

    Optional<UnitOfMeasureReference> findActiveUnitOfMeasure(String unitCode);

    Optional<CatalogItemReference> findActiveCatalogItemByCode(Long companyId, String itemCode);

    List<CatalogItemReference> findActiveCatalogItems(Long companyId);
}
