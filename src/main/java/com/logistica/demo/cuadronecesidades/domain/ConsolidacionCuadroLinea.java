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
@Table(name = "needs_consolidation_lines", schema = "cuadronecesidades")
public class ConsolidacionCuadroLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consolidation_id", nullable = false)
    private ConsolidacionCuadro consolidation;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private int fiscalYear;

    @Column(nullable = false)
    private Long costCenterId;

    @Column(nullable = false)
    private Long financingSourceId;

    @Column(nullable = false)
    private Long goalId;

    @Column(nullable = false)
    private Long expenseClassifierId;

    @Column(nullable = false)
    private Long catalogItemId;

    @Column(nullable = false)
    private Long unitOfMeasureId;

    @Column(nullable = false, length = 40)
    private String itemCode;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false, length = 20)
    private String unitCode;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal approvedQuantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedTotal;

    ConsolidacionCuadroLinea(
            ConsolidationKey key,
            BigDecimal approvedQuantity,
            BigDecimal estimatedTotal) {
        this.companyId = key.companyId();
        this.fiscalYear = key.fiscalYear();
        this.costCenterId = key.costCenterId();
        this.financingSourceId = key.financingSourceId();
        this.goalId = key.goalId();
        this.expenseClassifierId = key.expenseClassifierId();
        this.catalogItemId = key.catalogItemId();
        this.unitOfMeasureId = key.unitOfMeasureId();
        this.itemCode = key.itemCode();
        this.itemName = key.itemName();
        this.unitCode = key.unitCode();
        this.approvedQuantity = approvedQuantity;
        this.estimatedTotal = estimatedTotal;
    }

    void assignTo(ConsolidacionCuadro consolidation) {
        this.consolidation = consolidation;
    }

    record ConsolidationKey(
            Long companyId,
            int fiscalYear,
            Long costCenterId,
            Long financingSourceId,
            Long goalId,
            Long expenseClassifierId,
            Long catalogItemId,
            Long unitOfMeasureId,
            String itemCode,
            String itemName,
            String unitCode) {
    }
}
