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
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
@Table(name = "needs_plans", schema = "cuadronecesidades")
public class CuadroNecesidad extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCuadroNecesidad status = EstadoCuadroNecesidad.DRAFT;

    @Column(nullable = false, length = 200)
    private String title;

    private OffsetDateTime submittedAt;

    private OffsetDateTime reviewedAt;

    private OffsetDateTime consolidatedAt;

    @Version
    private Long version;

    @OrderBy("lineNumber asc")
    @OneToMany(mappedBy = "cuadro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CuadroNecesidadDetalle> details = new ArrayList<>();

    public CuadroNecesidad(
            Long companyId,
            int fiscalYear,
            Long costCenterId,
            Long financingSourceId,
            Long goalId,
            String title) {
        this.companyId = requireId(companyId, "companyId");
        if (fiscalYear < 2000 || fiscalYear > 2200) {
            throw new IllegalArgumentException("fiscalYear debe estar entre 2000 y 2200");
        }
        this.fiscalYear = fiscalYear;
        this.costCenterId = requireId(costCenterId, "costCenterId");
        this.financingSourceId = requireId(financingSourceId, "financingSourceId");
        this.goalId = requireId(goalId, "goalId");
        this.title = requireText(title, "title");
    }

    public void replaceDetails(List<CuadroNecesidadDetalle> newDetails) {
        requireEditable();
        if (newDetails == null || newDetails.isEmpty()) {
            throw new IllegalArgumentException("El cuadro debe tener al menos un detalle");
        }
        long distinctLineNumbers = newDetails.stream().map(CuadroNecesidadDetalle::getLineNumber).distinct().count();
        if (distinctLineNumbers != newDetails.size()) {
            throw new IllegalArgumentException("Los numeros de linea no pueden repetirse");
        }
        details.clear();
        newDetails.forEach(this::addDetail);
    }

    public void addDetail(CuadroNecesidadDetalle detail) {
        requireEditable();
        if (detail == null) {
            throw new IllegalArgumentException("detail es obligatorio");
        }
        boolean repeatedLineNumber = details.stream()
                .anyMatch(existing -> existing.getLineNumber() == detail.getLineNumber());
        if (repeatedLineNumber) {
            throw new IllegalArgumentException("Los numeros de linea no pueden repetirse");
        }
        detail.assignTo(this);
        details.add(detail);
    }

    public void submit(OffsetDateTime submittedAt) {
        if (status != EstadoCuadroNecesidad.DRAFT && status != EstadoCuadroNecesidad.OBSERVED) {
            throw new IllegalStateException("Solo se puede enviar un cuadro en estado DRAFT u OBSERVED");
        }
        if (details.isEmpty()) {
            throw new IllegalStateException("No se puede enviar un cuadro sin detalles");
        }
        this.status = EstadoCuadroNecesidad.SUBMITTED;
        this.submittedAt = requireInstant(submittedAt, "submittedAt");
    }

    public void observe(OffsetDateTime reviewedAt) {
        requireReviewDecisionAllowed("observar");
        this.status = EstadoCuadroNecesidad.OBSERVED;
        this.reviewedAt = requireInstant(reviewedAt, "reviewedAt");
    }

    public void markReviewed(OffsetDateTime reviewedAt) {
        requireReviewDecisionAllowed("revisar");
        requireAllDetailsReviewed();
        this.status = EstadoCuadroNecesidad.REVIEWED;
        this.reviewedAt = requireInstant(reviewedAt, "reviewedAt");
    }

    public void applyReview(List<CuadroNecesidadDetalle> reviewedDetails) {
        if (status != EstadoCuadroNecesidad.SUBMITTED) {
            throw new IllegalStateException("Solo se puede registrar revision de un cuadro en estado SUBMITTED");
        }
        if (reviewedDetails == null || reviewedDetails.size() != details.size()) {
            throw new IllegalArgumentException("La revision debe incluir todas las lineas del cuadro");
        }

        Map<Integer, CuadroNecesidadDetalle> reviewedByLine = reviewedDetails.stream()
                .collect(Collectors.toMap(CuadroNecesidadDetalle::getLineNumber, Function.identity()));
        if (!details.stream().map(CuadroNecesidadDetalle::getLineNumber).allMatch(reviewedByLine::containsKey)) {
            throw new IllegalArgumentException("La revision debe corresponder a las lineas del cuadro");
        }

        details.forEach(detail -> {
            CuadroNecesidadDetalle reviewed = reviewedByLine.get(detail.getLineNumber());
            detail.review(
                    reviewed.getReviewedQuantity(),
                    reviewed.getApprovedQuantity(),
                    reviewed.getMonthlyNeeds());
        });
    }

    public void reject(OffsetDateTime reviewedAt) {
        requireReviewDecisionAllowed("rechazar");
        this.status = EstadoCuadroNecesidad.REJECTED;
        this.reviewedAt = requireInstant(reviewedAt, "reviewedAt");
    }

    public boolean isEditable() {
        return status == EstadoCuadroNecesidad.DRAFT || status == EstadoCuadroNecesidad.OBSERVED;
    }

    private void requireEditable() {
        if (!isEditable()) {
            throw new IllegalStateException("Solo se puede editar un cuadro en estado DRAFT u OBSERVED");
        }
    }

    private void requireReviewDecisionAllowed(String action) {
        if (status != EstadoCuadroNecesidad.SUBMITTED) {
            throw new IllegalStateException("Solo se puede " + action + " un cuadro en estado SUBMITTED");
        }
    }

    private void requireAllDetailsReviewed() {
        boolean hasUnreviewedDetail = details.stream()
                .anyMatch(detail -> detail.getReviewedQuantity() == null || detail.getApprovedQuantity() == null);
        if (hasUnreviewedDetail) {
            throw new IllegalStateException("No se puede revisar un cuadro con lineas pendientes de revision");
        }
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

    private static OffsetDateTime requireInstant(OffsetDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }
}
