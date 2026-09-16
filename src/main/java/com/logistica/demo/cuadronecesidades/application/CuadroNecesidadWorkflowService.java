package com.logistica.demo.cuadronecesidades.application;

import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidadDetalle;
import com.logistica.demo.cuadronecesidades.domain.ProgramacionMensualNecesidad;
import com.logistica.demo.cuadronecesidades.domain.TipoVentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.VentanaCuadroNecesidad;
import com.logistica.demo.shared.exception.BusinessRuleException;
import java.time.OffsetDateTime;
import java.util.List;

public class CuadroNecesidadWorkflowService {

    public void replaceDetails(
            CuadroNecesidad cuadro,
            List<CuadroNecesidadDetalle> details,
            VentanaCuadroNecesidad registrationWindow,
            OffsetDateTime now) {
        requireOpenWindow(registrationWindow, TipoVentanaCuadroNecesidad.REGISTRATION, now);
        cuadro.replaceDetails(details);
    }

    public void submit(
            CuadroNecesidad cuadro,
            VentanaCuadroNecesidad registrationWindow,
            OffsetDateTime now) {
        requireOpenWindow(registrationWindow, TipoVentanaCuadroNecesidad.REGISTRATION, now);
        cuadro.submit(now);
    }

    public void observe(
            CuadroNecesidad cuadro,
            VentanaCuadroNecesidad reviewWindow,
            OffsetDateTime now) {
        requireOpenWindow(reviewWindow, TipoVentanaCuadroNecesidad.REVIEW, now);
        cuadro.observe(now);
    }

    public void markReviewed(
            CuadroNecesidad cuadro,
            List<RevisionLineaCuadro> revisions,
            VentanaCuadroNecesidad reviewWindow,
            OffsetDateTime now) {
        requireOpenWindow(reviewWindow, TipoVentanaCuadroNecesidad.REVIEW, now);
        cuadro.applyReview(toReviewedDetails(revisions));
        cuadro.markReviewed(now);
    }

    public void reject(
            CuadroNecesidad cuadro,
            VentanaCuadroNecesidad reviewWindow,
            OffsetDateTime now) {
        requireOpenWindow(reviewWindow, TipoVentanaCuadroNecesidad.REVIEW, now);
        cuadro.reject(now);
    }

    public ConsolidacionCuadro consolidate(
            Long companyId,
            int fiscalYear,
            List<CuadroNecesidad> plans,
            VentanaCuadroNecesidad consolidationWindow,
            OffsetDateTime now) {
        requireOpenWindow(consolidationWindow, TipoVentanaCuadroNecesidad.CONSOLIDATION, now);
        return ConsolidacionCuadro.consolidate(companyId, fiscalYear, plans, now);
    }

    public void reverseConsolidation(
            ConsolidacionCuadro consolidation,
            VentanaCuadroNecesidad consolidationWindow,
            OffsetDateTime now) {
        requireOpenWindow(consolidationWindow, TipoVentanaCuadroNecesidad.CONSOLIDATION, now);
        consolidation.reverse(now);
    }

    private void requireOpenWindow(
            VentanaCuadroNecesidad window,
            TipoVentanaCuadroNecesidad expectedType,
            OffsetDateTime now) {
        if (window == null || window.getWindowType() != expectedType || !window.isOpenAt(now)) {
            throw new BusinessRuleException("La ventana de " + expectedType.name() + " no esta abierta.");
        }
    }

    private List<CuadroNecesidadDetalle> toReviewedDetails(List<RevisionLineaCuadro> revisions) {
        if (revisions == null) {
            throw new IllegalArgumentException("revisions es obligatorio");
        }
        return revisions.stream()
                .map(revision -> CuadroNecesidadDetalle.reviewed(
                        revision.lineNumber(),
                        revision.reviewedQuantity(),
                        revision.approvedQuantity(),
                        revision.months().stream()
                                .map(month -> ProgramacionMensualNecesidad.reviewed(
                                        month.month(),
                                        month.reviewedQuantity(),
                                        month.approvedQuantity()))
                                .toList()))
                .toList();
    }
}
