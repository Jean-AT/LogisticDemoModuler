package com.logistica.demo.presupuesto.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

public class EjercicioPresupuestal {

    private final Long companyId;
    private final int fiscalYear;
    private final TipoEjercicioPresupuestal type;
    private EstadoEjercicioPresupuestal status = EstadoEjercicioPresupuestal.DRAFT;
    private OffsetDateTime approvedAt;
    private OffsetDateTime closedAt;

    public EjercicioPresupuestal(Long companyId, int fiscalYear, TipoEjercicioPresupuestal type) {
        this.companyId = requireId(companyId, "companyId");
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        this.fiscalYear = fiscalYear;
        this.type = Objects.requireNonNull(type, "type es obligatorio");
    }

    public void approve(OffsetDateTime approvedAt) {
        if (status != EstadoEjercicioPresupuestal.DRAFT) {
            throw new IllegalStateException("Solo se puede aprobar un ejercicio en estado DRAFT.");
        }
        this.approvedAt = Objects.requireNonNull(approvedAt, "approvedAt es obligatorio");
        this.status = EstadoEjercicioPresupuestal.APPROVED;
    }

    public void close(OffsetDateTime closedAt) {
        if (status != EstadoEjercicioPresupuestal.APPROVED) {
            throw new IllegalStateException("Solo se puede cerrar un ejercicio APPROVED.");
        }
        this.closedAt = Objects.requireNonNull(closedAt, "closedAt es obligatorio");
        this.status = EstadoEjercicioPresupuestal.CLOSED;
    }

    public boolean contributesToAvailability() {
        return status == EstadoEjercicioPresupuestal.APPROVED;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public TipoEjercicioPresupuestal getType() {
        return type;
    }

    public EstadoEjercicioPresupuestal getStatus() {
        return status;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    private static Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }
}
