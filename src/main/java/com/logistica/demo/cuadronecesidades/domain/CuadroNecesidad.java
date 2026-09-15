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
}
