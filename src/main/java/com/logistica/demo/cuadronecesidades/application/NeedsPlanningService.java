package com.logistica.demo.cuadronecesidades.application;

import com.logistica.demo.cuadronecesidades.api.NeedsBudgetTransferResult;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import com.logistica.demo.cuadronecesidades.api.NeedsBalanceQuery;
import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadroFuente;
import com.logistica.demo.cuadronecesidades.domain.ConsolidacionCuadroLinea;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.CuadroNecesidadDetalle;
import com.logistica.demo.cuadronecesidades.domain.EstadoConsolidacionCuadro;
import com.logistica.demo.cuadronecesidades.domain.EstadoCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.ProgramacionMensualNecesidad;
import com.logistica.demo.cuadronecesidades.domain.TipoVentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.domain.VentanaCuadroNecesidad;
import com.logistica.demo.cuadronecesidades.dto.ConsolidationLineResponse;
import com.logistica.demo.cuadronecesidades.dto.ConsolidationSourceResponse;
import com.logistica.demo.cuadronecesidades.dto.CreateNeedsPlanRequest;
import com.logistica.demo.cuadronecesidades.dto.MonthlyNeedResponse;
import com.logistica.demo.cuadronecesidades.dto.NeedLineRequest;
import com.logistica.demo.cuadronecesidades.dto.NeedLineResponse;
import com.logistica.demo.cuadronecesidades.dto.NeedsConsolidationResponse;
import com.logistica.demo.cuadronecesidades.dto.NeedsPlanResponse;
import com.logistica.demo.cuadronecesidades.dto.NeedsTraceabilityResponse;
import com.logistica.demo.cuadronecesidades.dto.ReplaceNeedsDetailsRequest;
import com.logistica.demo.cuadronecesidades.dto.ReviewMonthlyNeedRequest;
import com.logistica.demo.cuadronecesidades.dto.ReviewNeedLineRequest;
import com.logistica.demo.cuadronecesidades.dto.ReviewNeedsPlanRequest;
import com.logistica.demo.cuadronecesidades.infrastructure.persistence.ConsolidacionCuadroRepository;
import com.logistica.demo.cuadronecesidades.infrastructure.persistence.CuadroNecesidadRepository;
import com.logistica.demo.cuadronecesidades.infrastructure.persistence.VentanaCuadroNecesidadRepository;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.web.PageResponse;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NeedsPlanningService {

    private final CuadroNecesidadRepository plans;
    private final ConsolidacionCuadroRepository consolidations;
    private final VentanaCuadroNecesidadRepository windows;
    private final NeedsBalanceQuery balances;
    private final NeedsConsolidationTransferService transfers;
    private final CuadroNecesidadWorkflowService workflow;

    public NeedsPlanningService(
            CuadroNecesidadRepository plans,
            ConsolidacionCuadroRepository consolidations,
            VentanaCuadroNecesidadRepository windows,
            NeedsBalanceQuery balances,
            NeedsConsolidationTransferService transfers,
            CuadroNecesidadWorkflowService workflow) {
        this.plans = plans;
        this.consolidations = consolidations;
        this.windows = windows;
        this.balances = balances;
        this.transfers = transfers;
        this.workflow = workflow;
    }

    @Transactional(readOnly = true)
    public PageResponse<NeedsPlanResponse> listPlans(
            Long companyId,
            int fiscalYear,
            EstadoCuadroNecesidad status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = status == null
                ? plans.findByCompanyIdAndFiscalYear(companyId, fiscalYear, pageable)
                : plans.findByCompanyIdAndFiscalYearAndStatus(companyId, fiscalYear, status, pageable);
        return PageResponse.of(result.map(this::toPlanResponse));
    }

    @Transactional(readOnly = true)
    public NeedsPlanResponse getPlan(Long id) {
        return toPlanResponse(requirePlan(id));
    }

    @Transactional
    public NeedsPlanResponse createPlan(CreateNeedsPlanRequest request) {
        try {
            plans.findByCompanyIdAndFiscalYearAndCostCenterIdAndFinancingSourceIdAndGoalId(
                            request.companyId(),
                            request.fiscalYear(),
                            request.costCenterId(),
                            request.financingSourceId(),
                            request.goalId())
                    .ifPresent(existing -> {
                        throw new BusinessRuleException(
                                "Ya existe un cuadro de necesidades para la empresa, anio fiscal, centro de costo, fuente de financiamiento y meta indicados.");
                    });

            CuadroNecesidad plan = new CuadroNecesidad(
                    request.companyId(),
                    request.fiscalYear(),
                    request.costCenterId(),
                    request.financingSourceId(),
                    request.goalId(),
                    request.title());
            if (request.details() != null && !request.details().isEmpty()) {
                plan.replaceDetails(toDetails(request.details()));
            }
            return toPlanResponse(plans.save(plan));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsPlanResponse replaceDetails(Long planId, ReplaceNeedsDetailsRequest request, OffsetDateTime now) {
        CuadroNecesidad plan = requirePlan(planId);
        try {
            workflow.replaceDetails(
                    plan,
                    toDetails(request.details()),
                    requireWindow(plan, TipoVentanaCuadroNecesidad.REGISTRATION),
                    now);
            return toPlanResponse(plans.save(plan));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsPlanResponse submit(Long planId, OffsetDateTime now) {
        CuadroNecesidad plan = requirePlan(planId);
        try {
            workflow.submit(plan, requireWindow(plan, TipoVentanaCuadroNecesidad.REGISTRATION), now);
            return toPlanResponse(plans.save(plan));
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsPlanResponse review(Long planId, ReviewNeedsPlanRequest request, OffsetDateTime now) {
        CuadroNecesidad plan = requirePlan(planId);
        try {
            workflow.markReviewed(
                    plan,
                    toRevisions(request.revisions()),
                    requireWindow(plan, TipoVentanaCuadroNecesidad.REVIEW),
                    now);
            return toPlanResponse(plans.save(plan));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsPlanResponse observe(Long planId, OffsetDateTime now) {
        CuadroNecesidad plan = requirePlan(planId);
        try {
            workflow.observe(plan, requireWindow(plan, TipoVentanaCuadroNecesidad.REVIEW), now);
            return toPlanResponse(plans.save(plan));
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsPlanResponse reject(Long planId, OffsetDateTime now) {
        CuadroNecesidad plan = requirePlan(planId);
        try {
            workflow.reject(plan, requireWindow(plan, TipoVentanaCuadroNecesidad.REVIEW), now);
            return toPlanResponse(plans.save(plan));
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NeedsConsolidationResponse> listConsolidations(
            Long companyId,
            int fiscalYear,
            int page,
            int size) {
        var result = consolidations.findByCompanyIdAndFiscalYear(companyId, fiscalYear, PageRequest.of(page, size))
                .map(this::toConsolidationResponse);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public NeedsConsolidationResponse getConsolidation(Long id) {
        return toConsolidationResponse(requireConsolidation(id));
    }

    @Transactional
    public NeedsConsolidationResponse consolidate(Long companyId, int fiscalYear, OffsetDateTime now) {
        List<CuadroNecesidad> reviewedPlans = plans.findByCompanyIdAndFiscalYearAndStatus(
                companyId,
                fiscalYear,
                EstadoCuadroNecesidad.REVIEWED);
        try {
            ConsolidacionCuadro consolidation = workflow.consolidate(
                    companyId,
                    fiscalYear,
                    reviewedPlans,
                    requireWindow(companyId, fiscalYear, TipoVentanaCuadroNecesidad.CONSOLIDATION),
                    now);
            return toConsolidationResponse(consolidations.save(consolidation));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsConsolidationResponse reverseConsolidation(Long id, OffsetDateTime now) {
        ConsolidacionCuadro consolidation = requireConsolidation(id);
        try {
            workflow.reverseConsolidation(
                    consolidation,
                    requireWindow(consolidation.getCompanyId(), consolidation.getFiscalYear(), TipoVentanaCuadroNecesidad.CONSOLIDATION),
                    now);
            return toConsolidationResponse(consolidations.save(consolidation));
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }

    @Transactional
    public NeedsBudgetTransferResult transfer(Long id, IdempotencyKey idempotencyKey, String actor, OffsetDateTime now) {
        return transfers.transfer(id, idempotencyKey, actor, now);
    }

    @Transactional(readOnly = true)
    public NeedsLineBalance findBalance(Long companyId, Long lineId) {
        return balances.findAvailableLine(companyId, lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo de linea de Cuadro no encontrado"));
    }

    @Transactional(readOnly = true)
    public NeedsTraceabilityResponse tracePlan(Long planId) {
        CuadroNecesidad plan = requirePlan(planId);
        List<NeedsConsolidationResponse> related = consolidations
                .findAllByCompanyIdAndFiscalYearAndStatusIn(
                        plan.getCompanyId(),
                        plan.getFiscalYear(),
                        List.of(
                                EstadoConsolidacionCuadro.CONSOLIDATED,
                                EstadoConsolidacionCuadro.REVERSED,
                                EstadoConsolidacionCuadro.TRANSFERRED))
                .stream()
                .filter(consolidation -> consolidation.getSources().stream()
                        .map(ConsolidacionCuadroFuente::getNeedsPlanId)
                        .anyMatch(plan.getId()::equals))
                .map(this::toConsolidationResponse)
                .toList();
        return new NeedsTraceabilityResponse(toPlanResponse(plan), related);
    }

    private CuadroNecesidad requirePlan(Long id) {
        return plans.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuadro de Necesidades no encontrado: " + id));
    }

    private ConsolidacionCuadro requireConsolidation(Long id) {
        return consolidations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consolidacion no encontrada: " + id));
    }

    private VentanaCuadroNecesidad requireWindow(CuadroNecesidad plan, TipoVentanaCuadroNecesidad type) {
        return requireWindow(plan.getCompanyId(), plan.getFiscalYear(), type);
    }

    private VentanaCuadroNecesidad requireWindow(Long companyId, int fiscalYear, TipoVentanaCuadroNecesidad type) {
        return windows.findByCompanyIdAndFiscalYearAndWindowTypeAndActiveTrue(companyId, fiscalYear, type)
                .orElseThrow(() -> new BusinessRuleException("No existe una ventana activa de " + type.name() + "."));
    }

    private List<CuadroNecesidadDetalle> toDetails(List<NeedLineRequest> details) {
        if (details == null) {
            throw new BadRequestException("details es obligatorio");
        }
        return details.stream()
                .map(detail -> new CuadroNecesidadDetalle(
                        detail.lineNumber(),
                        detail.catalogItemId(),
                        detail.expenseClassifierId(),
                        detail.unitOfMeasureId(),
                        detail.itemCode(),
                        detail.itemName(),
                        detail.unitCode(),
                        detail.requestedQuantity(),
                        detail.estimatedUnitPrice(),
                        detail.months().stream()
                                .map(month -> new ProgramacionMensualNecesidad(
                                        month.month(),
                                        month.requestedQuantity()))
                                .toList()))
                .toList();
    }

    private List<RevisionLineaCuadro> toRevisions(List<ReviewNeedLineRequest> revisions) {
        if (revisions == null) {
            throw new BadRequestException("revisions es obligatorio");
        }
        return revisions.stream()
                .map(revision -> new RevisionLineaCuadro(
                        revision.lineNumber(),
                        revision.reviewedQuantity(),
                        revision.approvedQuantity(),
                        revision.months().stream()
                                .map(this::toRevisionMonth)
                                .toList()))
                .toList();
    }

    private RevisionMensualCuadro toRevisionMonth(ReviewMonthlyNeedRequest month) {
        return new RevisionMensualCuadro(month.month(), month.reviewedQuantity(), month.approvedQuantity());
    }

    private NeedsPlanResponse toPlanResponse(CuadroNecesidad plan) {
        return new NeedsPlanResponse(
                plan.getId(),
                plan.getCompanyId(),
                plan.getFiscalYear(),
                plan.getCostCenterId(),
                plan.getFinancingSourceId(),
                plan.getGoalId(),
                plan.getStatus(),
                plan.getTitle(),
                plan.getSubmittedAt(),
                plan.getReviewedAt(),
                plan.getConsolidatedAt(),
                plan.getDetails().stream()
                        .sorted(Comparator.comparingInt(CuadroNecesidadDetalle::getLineNumber))
                        .map(this::toLineResponse)
                        .toList());
    }

    private NeedLineResponse toLineResponse(CuadroNecesidadDetalle detail) {
        return new NeedLineResponse(
                detail.getId(),
                detail.getLineNumber(),
                detail.getCatalogItemId(),
                detail.getExpenseClassifierId(),
                detail.getUnitOfMeasureId(),
                detail.getItemCode(),
                detail.getItemName(),
                detail.getUnitCode(),
                detail.getRequestedQuantity(),
                detail.getReviewedQuantity(),
                detail.getApprovedQuantity(),
                detail.getEstimatedUnitPrice(),
                detail.getEstimatedTotal(),
                detail.getConsumedQuantity(),
                detail.getMonthlyNeeds().stream()
                        .sorted(Comparator.comparingInt(ProgramacionMensualNecesidad::getMonth))
                        .map(month -> new MonthlyNeedResponse(
                                month.getMonth(),
                                month.getRequestedQuantity(),
                                month.getReviewedQuantity(),
                                month.getApprovedQuantity(),
                                month.getConsumedQuantity()))
                        .toList());
    }

    private NeedsConsolidationResponse toConsolidationResponse(ConsolidacionCuadro consolidation) {
        return new NeedsConsolidationResponse(
                consolidation.getId(),
                consolidation.getCompanyId(),
                consolidation.getFiscalYear(),
                consolidation.getStatus(),
                consolidation.getConsolidatedAt(),
                consolidation.getReversedAt(),
                consolidation.getTransferredAt(),
                consolidation.getTransferId(),
                consolidation.getUnitBudgetExerciseId(),
                consolidation.getSources().stream()
                        .map(source -> new ConsolidationSourceResponse(
                                source.getNeedsPlanId(),
                                source.getCompanyId(),
                                source.getFiscalYear(),
                                source.getCostCenterId(),
                                source.getFinancingSourceId(),
                                source.getGoalId()))
                        .toList(),
                consolidation.getLines().stream()
                        .map(this::toConsolidationLineResponse)
                        .toList());
    }

    private ConsolidationLineResponse toConsolidationLineResponse(ConsolidacionCuadroLinea line) {
        return new ConsolidationLineResponse(
                line.getId(),
                line.getCostCenterId(),
                line.getFinancingSourceId(),
                line.getGoalId(),
                line.getExpenseClassifierId(),
                line.getCatalogItemId(),
                line.getUnitOfMeasureId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUnitCode(),
                line.getApprovedQuantity(),
                line.getEstimatedTotal());
    }
}
