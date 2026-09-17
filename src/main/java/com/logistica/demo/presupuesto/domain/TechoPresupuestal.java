package com.logistica.demo.presupuesto.domain;

import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import java.util.Objects;

public record TechoPresupuestal(FiscalDimension dimension, Money amount) {

    public TechoPresupuestal {
        Objects.requireNonNull(dimension, "dimension es obligatoria");
        Objects.requireNonNull(amount, "amount es obligatorio");
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException("El techo no puede ser negativo.");
        }
    }

    public boolean covers(Money requested) {
        Objects.requireNonNull(requested, "requested es obligatorio");
        if (requested.currency() != amount.currency()) {
            throw new IllegalArgumentException("El techo y el importe solicitado deben usar la misma moneda.");
        }
        return requested.amount().compareTo(amount.amount()) <= 0;
    }
}
