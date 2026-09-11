package com.logistica.demo.platform.api;

import java.util.List;
import java.util.Optional;

public interface PlatformCatalogQuery {

    Optional<MasterDataReference> findActiveCompany(Long companyId);

    Optional<MasterDataReference> findActiveCostCenter(Long companyId, Long costCenterId);

    Optional<MasterDataReference> findActiveFinancingSource(Long financingSourceId);

    Optional<MasterDataReference> findActiveGoal(Long goalId);

    Optional<MasterDataReference> findActiveExpenseClassifier(Long expenseClassifierId);

    Optional<MasterDataReference> findActiveCatalogItem(Long catalogItemId);

    Optional<CurrencyReference> findActiveCurrency(String currencyCode);

    Optional<UnitOfMeasureReference> findActiveUnitOfMeasure(String unitCode);

    Optional<CatalogItemReference> findActiveCatalogItemByCode(Long companyId, String itemCode);

    List<CatalogItemReference> findActiveCatalogItems(Long companyId);
}
