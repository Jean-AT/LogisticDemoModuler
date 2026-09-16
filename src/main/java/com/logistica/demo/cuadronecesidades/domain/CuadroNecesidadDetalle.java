package com.logistica.demo.cuadronecesidades.domain;

import com.logistica.demo.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "need_lines", schema = "cuadronecesidades")
public class CuadroNecesidadDetalle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "needs_plan_id", nullable = false)
    private CuadroNecesidad cuadro;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private int lineNumber;

    @Column(nullable = false)
    private Long catalogItemId;

    @Column(nullable = false)
    private Long expenseClassifierId;

    @Column(nullable = false)
    private Long unitOfMeasureId;

    @Column(nullable = false, length = 40)
    private String itemCode;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false, length = 20)
    private String unitCode;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal requestedQuantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal reviewedQuantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal approvedQuantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedUnitPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedTotal;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal consumedQuantity = BigDecimal.ZERO;

    @Version
    private Long version;

    @OrderBy("month asc")
    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramacionMensualNecesidad> monthlyNeeds = new ArrayList<>();

    public CuadroNecesidadDetalle(
            int lineNumber,
            Long catalogItemId,
            Long expenseClassifierId,
            Long unitOfMeasureId,
            String itemCode,
            String itemName,
            String unitCode,
            BigDecimal requestedQuantity,
            BigDecimal estimatedUnitPrice,
            List<ProgramacionMensualNecesidad> monthlyNeeds) {
        if (lineNumber <= 0) {
            throw new IllegalArgumentException("lineNumber debe ser positivo");
        }
        this.lineNumber = lineNumber;
        this.catalogItemId = requireId(catalogItemId, "catalogItemId");
        this.expenseClassifierId = requireId(expenseClassifierId, "expenseClassifierId");
        this.unitOfMeasureId = requireId(unitOfMeasureId, "unitOfMeasureId");
        this.itemCode = requireText(itemCode, "itemCode");
        this.itemName = requireText(itemName, "itemName");
        this.unitCode = requireText(unitCode, "unitCode");
        this.requestedQuantity = requirePositive(requestedQuantity, "requestedQuantity");
        this.estimatedUnitPrice = requireNonNegative(estimatedUnitPrice, "estimatedUnitPrice");
        this.estimatedTotal = this.requestedQuantity.multiply(this.estimatedUnitPrice);
        replaceMonthlyNeeds(monthlyNeeds);
    }

    public static CuadroNecesidadDetalle reviewed(
            int lineNumber,
            BigDecimal reviewedQuantity,
            BigDecimal approvedQuantity,
            List<ProgramacionMensualNecesidad> reviewedMonths) {
        CuadroNecesidadDetalle detail = new CuadroNecesidadDetalle();
        detail.lineNumber = lineNumber;
        detail.review(reviewedQuantity, approvedQuantity, reviewedMonths);
        return detail;
    }

    void assignTo(CuadroNecesidad cuadro) {
        this.cuadro = cuadro;
        this.companyId = cuadro.getCompanyId();
    }

    public void replaceMonthlyNeeds(List<ProgramacionMensualNecesidad> monthlyNeeds) {
        if (monthlyNeeds == null || monthlyNeeds.size() != 12) {
            throw new IllegalArgumentException("La programacion mensual debe contener los 12 meses");
        }
        List<Integer> months = monthlyNeeds.stream()
                .map(ProgramacionMensualNecesidad::getMonth)
                .sorted()
                .toList();
        if (!months.equals(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList())) {
            throw new IllegalArgumentException("La programacion mensual debe cubrir los meses 1 al 12");
        }
        BigDecimal monthlyTotal = monthlyNeeds.stream()
                .map(ProgramacionMensualNecesidad::getRequestedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monthlyTotal.compareTo(requestedQuantity) != 0) {
            throw new IllegalArgumentException("La suma mensual debe coincidir con la cantidad anual solicitada");
        }
        this.monthlyNeeds.clear();
        monthlyNeeds.stream()
                .sorted(Comparator.comparingInt(ProgramacionMensualNecesidad::getMonth))
                .forEach(month -> {
                    month.assignTo(this);
                    this.monthlyNeeds.add(month);
                });
    }

    public void review(
            BigDecimal reviewedQuantity,
            BigDecimal approvedQuantity,
            List<ProgramacionMensualNecesidad> reviewedMonths) {
        ProgramacionMensualNecesidad.requireNonNegative(reviewedQuantity, "reviewedQuantity");
        ProgramacionMensualNecesidad.requireNonNegative(approvedQuantity, "approvedQuantity");
        if (approvedQuantity.compareTo(reviewedQuantity) > 0) {
            throw new IllegalArgumentException("approvedQuantity no puede superar reviewedQuantity");
        }
        if (reviewedMonths == null || reviewedMonths.size() != 12) {
            throw new IllegalArgumentException("La revision mensual debe contener los 12 meses");
        }

        Map<Integer, ProgramacionMensualNecesidad> reviewedByMonth = reviewedMonths.stream()
                .collect(Collectors.toMap(ProgramacionMensualNecesidad::getMonth, Function.identity()));
        if (!reviewedByMonth.keySet().stream().sorted().toList()
                .equals(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList())) {
            throw new IllegalArgumentException("La revision mensual debe cubrir los meses 1 al 12");
        }

        BigDecimal monthlyReviewedTotal = reviewedMonths.stream()
                .map(ProgramacionMensualNecesidad::getReviewedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyApprovedTotal = reviewedMonths.stream()
                .map(ProgramacionMensualNecesidad::getApprovedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monthlyReviewedTotal.compareTo(reviewedQuantity) != 0) {
            throw new IllegalArgumentException("La suma mensual revisada debe coincidir con la cantidad revisada anual");
        }
        if (monthlyApprovedTotal.compareTo(approvedQuantity) != 0) {
            throw new IllegalArgumentException("La suma mensual aprobada debe coincidir con la cantidad aprobada anual");
        }

        if (monthlyNeeds.isEmpty()) {
            reviewedMonths.stream()
                    .sorted(Comparator.comparingInt(ProgramacionMensualNecesidad::getMonth))
                    .forEach(month -> {
                        month.assignTo(this);
                        monthlyNeeds.add(month);
                    });
        } else {
            monthlyNeeds.forEach(month -> {
                ProgramacionMensualNecesidad reviewedMonth = reviewedByMonth.get(month.getMonth());
                month.review(reviewedMonth.getReviewedQuantity(), reviewedMonth.getApprovedQuantity());
            });
        }
        this.reviewedQuantity = reviewedQuantity;
        this.approvedQuantity = approvedQuantity;
    }

    BigDecimal approvedEstimatedTotal() {
        if (approvedQuantity == null) {
            throw new IllegalStateException("La linea no tiene cantidad aprobada");
        }
        return approvedQuantity.multiply(estimatedUnitPrice);
    }

    private static Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " debe ser mayor que cero");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " debe ser mayor o igual a cero");
        }
        return value;
    }
}
