package com.logistica.demo.cuadronecesidades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "monthly_needs", schema = "cuadronecesidades")
public class ProgramacionMensualNecesidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "need_line_id", nullable = false)
    private CuadroNecesidadDetalle detalle;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal requestedQuantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal reviewedQuantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal approvedQuantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal consumedQuantity = BigDecimal.ZERO;

    public ProgramacionMensualNecesidad(int month, BigDecimal requestedQuantity) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month debe estar entre 1 y 12");
        }
        requireNonNegative(requestedQuantity, "requestedQuantity");
        this.month = month;
        this.requestedQuantity = requestedQuantity;
    }

    void assignTo(CuadroNecesidadDetalle detalle) {
        this.detalle = detalle;
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " debe ser mayor o igual a cero");
        }
    }
}
