package com.logistica.demo.presupuesto.domain;

import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import java.time.OffsetDateTime;
import java.util.Objects;

public record MovimientoPresupuestal(
        TipoMovimientoPresupuestal type,
        FiscalDimension dimension,
        Money amount,
        DocumentReference source,
        OffsetDateTime registeredAt,
        String actor) {

    public MovimientoPresupuestal {
        Objects.requireNonNull(type, "type es obligatorio");
        Objects.requireNonNull(dimension, "dimension es obligatoria");
        Objects.requireNonNull(amount, "amount es obligatorio");
        Objects.requireNonNull(source, "source es obligatorio");
        Objects.requireNonNull(registeredAt, "registeredAt es obligatorio");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("El movimiento debe tener importe positivo.");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor es obligatorio");
        }
        actor = actor.trim();
    }
}
