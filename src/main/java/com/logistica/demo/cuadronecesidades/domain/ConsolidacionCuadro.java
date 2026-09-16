package com.logistica.demo.cuadronecesidades.domain;

import com.logistica.demo.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "needs_consolidations", schema = "cuadronecesidades")
public class ConsolidacionCuadro extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private int fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoConsolidacionCuadro status;

    @Column(nullable = false)
    private OffsetDateTime consolidatedAt;

    private OffsetDateTime reversedAt;

    private OffsetDateTime transferredAt;

    @Version
    private Long version;

    @OrderBy("id asc")
    @OneToMany(mappedBy = "consolidation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsolidacionCuadroFuente> sources = new ArrayList<>();

    @OrderBy("id asc")
    @OneToMany(mappedBy = "consolidation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsolidacionCuadroLinea> lines = new ArrayList<>();

    public static ConsolidacionCuadro consolidate(
            Long companyId,
            int fiscalYear,
            List<CuadroNecesidad> plans,
            OffsetDateTime consolidatedAt) {
        if (plans == null || plans.isEmpty()) {
            throw new IllegalArgumentException("La consolidacion requiere al menos un cuadro revisado");
        }
        ConsolidacionCuadro consolidation = new ConsolidacionCuadro();
        consolidation.companyId = requireId(companyId, "companyId");
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        consolidation.fiscalYear = fiscalYear;
        consolidation.status = EstadoConsolidacionCuadro.CONSOLIDATED;
        consolidation.consolidatedAt = requireInstant(consolidatedAt, "consolidatedAt");
        consolidation.addPlans(plans);
        return consolidation;
    }

    public void reverse(OffsetDateTime reversedAt) {
        if (status != EstadoConsolidacionCuadro.CONSOLIDATED) {
            throw new IllegalStateException("Solo se puede revertir una consolidacion no transferida");
        }
        status = EstadoConsolidacionCuadro.REVERSED;
        this.reversedAt = requireInstant(reversedAt, "reversedAt");
        sources.forEach(source -> source.getPlan().revertConsolidation());
    }

    private void addPlans(List<CuadroNecesidad> plans) {
        Map<ConsolidacionCuadroLinea.ConsolidationKey, Totals> totalsByKey = new LinkedHashMap<>();
        plans.forEach(plan -> {
            validatePlan(plan);
            ConsolidacionCuadroFuente source = new ConsolidacionCuadroFuente(plan);
            source.assignTo(this);
            sources.add(source);
            plan.markConsolidated(consolidatedAt);

            plan.getDetails().forEach(detail -> {
                ConsolidacionCuadroLinea.ConsolidationKey key = new ConsolidacionCuadroLinea.ConsolidationKey(
                        plan.getCompanyId(),
                        plan.getFiscalYear(),
                        plan.getCostCenterId(),
                        plan.getFinancingSourceId(),
                        plan.getGoalId(),
                        detail.getExpenseClassifierId(),
                        detail.getCatalogItemId(),
                        detail.getUnitOfMeasureId(),
                        detail.getItemCode(),
                        detail.getItemName(),
                        detail.getUnitCode());
                totalsByKey.computeIfAbsent(key, ignored -> new Totals())
                        .add(detail.getApprovedQuantity(), detail.approvedEstimatedTotal());
            });
        });

        totalsByKey.forEach((key, totals) -> {
            ConsolidacionCuadroLinea line = new ConsolidacionCuadroLinea(
                    key,
                    totals.approvedQuantity,
                    totals.estimatedTotal);
            line.assignTo(this);
            lines.add(line);
        });
    }

    private void validatePlan(CuadroNecesidad plan) {
        if (plan == null) {
            throw new IllegalArgumentException("El cuadro es obligatorio");
        }
        if (!companyId.equals(plan.getCompanyId()) || fiscalYear != plan.getFiscalYear()) {
            throw new IllegalArgumentException("Todos los cuadros deben pertenecer a la misma compania y ejercicio");
        }
        if (plan.getStatus() != EstadoCuadroNecesidad.REVIEWED) {
            throw new IllegalStateException("Solo se pueden consolidar cuadros en estado REVIEWED");
        }
    }

    private static Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    private static OffsetDateTime requireInstant(OffsetDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    private static class Totals {
        private BigDecimal approvedQuantity = BigDecimal.ZERO;
        private BigDecimal estimatedTotal = BigDecimal.ZERO;

        void add(BigDecimal approvedQuantity, BigDecimal estimatedTotal) {
            this.approvedQuantity = this.approvedQuantity.add(approvedQuantity);
            this.estimatedTotal = this.estimatedTotal.add(estimatedTotal);
        }
    }
}
