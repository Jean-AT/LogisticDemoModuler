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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "needs_consolidation_sources", schema = "cuadronecesidades")
public class ConsolidacionCuadroFuente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consolidation_id", nullable = false)
    private ConsolidacionCuadro consolidation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "needs_plan_id", nullable = false, insertable = false, updatable = false)
    private CuadroNecesidad plan;

    @Column(name = "needs_plan_id", nullable = false)
    private Long needsPlanId;

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

    ConsolidacionCuadroFuente(CuadroNecesidad plan) {
        this.plan = plan;
        this.needsPlanId = plan.getId();
        this.companyId = plan.getCompanyId();
        this.fiscalYear = plan.getFiscalYear();
        this.costCenterId = plan.getCostCenterId();
        this.financingSourceId = plan.getFinancingSourceId();
        this.goalId = plan.getGoalId();
    }

    void assignTo(ConsolidacionCuadro consolidation) {
        this.consolidation = consolidation;
    }
}
