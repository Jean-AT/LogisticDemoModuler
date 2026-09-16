package com.logistica.demo.cuadronecesidades.domain;

import com.logistica.demo.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "needs_windows", schema = "cuadronecesidades")
public class VentanaCuadroNecesidad extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private int fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoVentanaCuadroNecesidad windowType;

    @Column(nullable = false)
    private OffsetDateTime opensAt;

    @Column(nullable = false)
    private OffsetDateTime closesAt;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;

    public VentanaCuadroNecesidad(
            Long companyId,
            int fiscalYear,
            TipoVentanaCuadroNecesidad windowType,
            OffsetDateTime opensAt,
            OffsetDateTime closesAt) {
        this.companyId = requireId(companyId, "companyId");
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        this.fiscalYear = fiscalYear;
        if (windowType == null) {
            throw new IllegalArgumentException("windowType es obligatorio");
        }
        this.windowType = windowType;
        if (opensAt == null || closesAt == null || !closesAt.isAfter(opensAt)) {
            throw new IllegalArgumentException("La ventana debe cerrar despues de abrir");
        }
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public boolean isOpenAt(OffsetDateTime instant) {
        return active
                && instant != null
                && !instant.isBefore(opensAt)
                && instant.isBefore(closesAt);
    }

    private static Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }
}
