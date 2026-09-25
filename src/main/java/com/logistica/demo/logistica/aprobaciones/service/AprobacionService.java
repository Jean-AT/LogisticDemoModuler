package com.logistica.demo.logistica.aprobaciones.service;

import com.logistica.demo.logistica.aprobaciones.domain.AccionAprobacion;
import com.logistica.demo.logistica.aprobaciones.domain.Aprobacion;
import com.logistica.demo.logistica.aprobaciones.dto.AprobacionDecisionRequest;
import com.logistica.demo.logistica.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.logistica.requerimientos.dto.RequerimientoResponse;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.logistica.requerimientos.service.RequerimientoService;
import com.logistica.demo.cuadronecesidades.api.MonthlyNeedBalance;
import com.logistica.demo.cuadronecesidades.api.NeedsBalanceQuery;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import com.logistica.demo.presupuesto.api.BudgetAllocation;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.PrecommitBudgetCommand;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import com.logistica.demo.sharedkernel.web.PageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobacionService {

    private final RequerimientoRepository requerimientoRepository;
    private final RequerimientoService requerimientoService;
    private final BudgetControlUseCase budgetControlUseCase;
    private final NeedsBalanceQuery needsBalanceQuery;
    private final CurrentUserService currentUserService;

    public AprobacionService(
            RequerimientoRepository requerimientoRepository,
            RequerimientoService requerimientoService,
            BudgetControlUseCase budgetControlUseCase,
            NeedsBalanceQuery needsBalanceQuery,
            CurrentUserService currentUserService) {
        this.requerimientoRepository = requerimientoRepository;
        this.requerimientoService = requerimientoService;
        this.budgetControlUseCase = budgetControlUseCase;
        this.needsBalanceQuery = needsBalanceQuery;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public RequerimientoResponse aprobar(Long requerimientoId, AprobacionDecisionRequest request) {
        return aplicarDecision(requerimientoId, AccionAprobacion.APROBAR, request);
    }

    @Transactional
    public RequerimientoResponse observar(Long requerimientoId, AprobacionDecisionRequest request) {
        return aplicarDecision(requerimientoId, AccionAprobacion.OBSERVAR, request);
    }

    @Transactional
    public RequerimientoResponse rechazar(Long requerimientoId, AprobacionDecisionRequest request) {
        return aplicarDecision(requerimientoId, AccionAprobacion.RECHAZAR, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequerimientoResponse> list(
            Long id,
            EstadoRequerimiento estado,
            String numero,
            Long proveedorId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {
        return requerimientoService.list(id, estado, numero, proveedorId, fechaDesde, fechaHasta, page, size);
    }
    @Transactional(readOnly = true)
    public RequerimientoResponse getByRequerimientoId(Long requerimientoId) {
        return requerimientoService.getById(requerimientoId);
    }

    private RequerimientoResponse aplicarDecision(
            Long requerimientoId,
            AccionAprobacion accion,
            AprobacionDecisionRequest request) {
        Requerimiento requerimiento = requerimientoRepository.findById(requerimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento no encontrado."));

        if (requerimiento.getEstado() != EstadoRequerimiento.ENVIADO
                && requerimiento.getEstado() != EstadoRequerimiento.OBSERVADO) {
            throw new BusinessRuleException("Solo se puede decidir un requerimiento en estado ENVIADO u OBSERVADO.");
        }

        String comentario = request != null ? request.comentario() : null;
        if ((accion == AccionAprobacion.OBSERVAR || accion == AccionAprobacion.RECHAZAR)
                && (comentario == null || comentario.isBlank())) {
            throw new BusinessRuleException("El comentario es obligatorio para observar o rechazar.");
        }

        Aprobacion aprobacion = new Aprobacion();
        aprobacion.setAccion(accion);
        aprobacion.setComentario(comentario == null ? null : comentario.trim());
        aprobacion.setDecisionAt(LocalDateTime.now());
        requerimiento.addAprobacion(aprobacion);
        if (accion == AccionAprobacion.APROBAR) {
            precommitBudgetIfNeeded(requerimiento);
        }
        requerimiento.cambiarEstado(resolveEstado(accion), comentario);

        return requerimientoService.mapResponse(requerimientoRepository.saveAndFlush(requerimiento));
    }

    private void precommitBudgetIfNeeded(Requerimiento requerimiento) {
        if (requerimiento.getBudgetControlId() != null) {
            return;
        }
        if (requerimiento.getNeedsLineId() == null) {
            throw new BusinessRuleException(
                    "El requerimiento no tiene trazabilidad presupuestal. Cree el requerimiento desde Cuadro o vincule presupuesto antes de aprobar.");
        }
        if (requerimiento.getCompanyId() == null || requerimiento.getFiscalYear() == null) {
            throw new BusinessRuleException("El requerimiento no tiene trazabilidad presupuestal completa.");
        }

        List<BudgetAllocation> allocations = buildBudgetAllocations(requerimiento);
        BudgetControlResult result = budgetControlUseCase.precommit(new PrecommitBudgetCommand(
                new DocumentReference(
                        "LOGISTICA",
                        "REQUERIMIENTO",
                        requerimiento.getId(),
                        requerimiento.getNumero()),
                allocations,
                new IdempotencyKey("logistica-requerimiento-precommit-" + requerimiento.getId()),
                currentUserService.getUsername()));
        requerimiento.setBudgetControlId(result.budgetControlId());
    }

    private List<BudgetAllocation> buildBudgetAllocations(Requerimiento requerimiento) {
        List<BudgetAllocation> allocations = new ArrayList<>();
        for (RequerimientoDetalle detalle : requerimiento.getDetalles()) {
            if (detalle.getNeedsLineId() == null) {
                throw new BusinessRuleException("Todos los detalles deben provenir de una linea de Cuadro.");
            }
            NeedsLineBalance balance = needsBalanceQuery
                    .findAvailableLine(requerimiento.getCompanyId(), detalle.getNeedsLineId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "La linea de Cuadro no tiene saldo disponible para precomprometer."));
            allocations.addAll(splitDetailAcrossMonths(requerimiento, detalle, balance));
        }
        if (allocations.isEmpty()) {
            throw new BusinessRuleException("El requerimiento no tiene importes para precomprometer.");
        }
        return allocations;
    }

    private List<BudgetAllocation> splitDetailAcrossMonths(
            Requerimiento requerimiento,
            RequerimientoDetalle detalle,
            NeedsLineBalance balance) {
        BigDecimal remainingQuantity = BigDecimal.valueOf(detalle.getCantidad());
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        List<BudgetAllocation> allocations = new ArrayList<>();
        List<MonthlyNeedBalance> months = balance.months().stream()
                .filter(month -> month.availableQuantity() != null && month.availableQuantity().signum() > 0)
                .sorted(Comparator.comparingInt(MonthlyNeedBalance::month))
                .toList();
        if (months.isEmpty()) {
            throw new BusinessRuleException("La linea de Cuadro no tiene saldo mensual disponible.");
        }

        for (MonthlyNeedBalance month : months) {
            if (remainingQuantity.signum() == 0) {
                break;
            }
            BigDecimal quantity = remainingQuantity.min(month.availableQuantity());
            remainingQuantity = remainingQuantity.subtract(quantity);
            BigDecimal amount = remainingQuantity.signum() == 0
                    ? detalle.getSubtotalLinea().subtract(allocatedAmount)
                    : detalle.getPrecioUnitarioEstimado().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            allocatedAmount = allocatedAmount.add(amount);
            allocations.add(new BudgetAllocation(
                    new FiscalDimension(
                            balance.companyId(),
                            balance.fiscalYear(),
                            month.month(),
                            balance.costCenterId(),
                            balance.financingSourceId(),
                            balance.goalId(),
                            balance.expenseClassifierId()),
                    new Money(amount, requerimiento.getMoneda())));
        }

        if (remainingQuantity.signum() > 0) {
            throw new BusinessRuleException(
                    "La cantidad solicitada supera el saldo mensual disponible de la linea de Cuadro.");
        }
        return allocations;
    }

    private EstadoRequerimiento resolveEstado(AccionAprobacion accion) {
        return switch (accion) {
            case APROBAR -> EstadoRequerimiento.APROBADO;
            case OBSERVAR -> EstadoRequerimiento.OBSERVADO;
            case RECHAZAR -> EstadoRequerimiento.RECHAZADO;
        };
    }
}
