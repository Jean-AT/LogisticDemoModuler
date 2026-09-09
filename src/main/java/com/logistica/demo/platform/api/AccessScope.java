package com.logistica.demo.platform.api;

import java.util.Objects;

public record AccessScope(Long companyId, Long organizationUnitId, Long costCenterId) {

    public AccessScope {
        Objects.requireNonNull(companyId, "companyId es obligatorio");
        if (companyId <= 0) {
            throw new IllegalArgumentException("companyId debe ser positivo");
        }
        if (organizationUnitId != null && organizationUnitId <= 0) {
            throw new IllegalArgumentException("organizationUnitId debe ser positivo");
        }
        if (costCenterId != null && costCenterId <= 0) {
            throw new IllegalArgumentException("costCenterId debe ser positivo");
        }
    }

    public boolean covers(Long requestedCompanyId, Long requestedOrganizationUnitId, Long requestedCostCenterId) {
        if (!companyId.equals(requestedCompanyId)) {
            return false;
        }
        if (organizationUnitId != null && !organizationUnitId.equals(requestedOrganizationUnitId)) {
            return false;
        }
        return costCenterId == null || costCenterId.equals(requestedCostCenterId);
    }
}
