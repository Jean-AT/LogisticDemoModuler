package com.logistica.demo.presupuesto.dto;

import com.logistica.demo.presupuesto.api.BudgetAvailability;
import java.math.BigDecimal;

public record BudgetAvailabilityResponse(
        Long companyId,
        int fiscalYear,
        int month,
        Long costCenterId,
        Long financingSourceId,
        Long goalId,
        Long expenseClassifierId,
        String currency,
        BigDecimal assigned,
        BigDecimal precommitted,
        BigDecimal committed,
        BigDecimal available) {

    public static BudgetAvailabilityResponse from(BudgetAvailability availability) {
        return new BudgetAvailabilityResponse(
                availability.dimension().companyId(),
                availability.dimension().year(),
                availability.dimension().month(),
                availability.dimension().costCenterId(),
                availability.dimension().financingSourceId(),
                availability.dimension().goalId(),
                availability.dimension().expenseClassifierId(),
                availability.assigned().currency().name(),
                availability.assigned().amount(),
                availability.precommitted().amount(),
                availability.committed().amount(),
                availability.available().amount());
    }
}
