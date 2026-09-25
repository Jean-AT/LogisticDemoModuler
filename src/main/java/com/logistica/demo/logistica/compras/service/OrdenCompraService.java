package com.logistica.demo.logistica.compras.service;

import com.logistica.demo.cuadronecesidades.api.MonthlyNeedBalance;
import com.logistica.demo.cuadronecesidades.api.NeedsBalanceQuery;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import com.logistica.demo.logistica.compras.domain.Adjudicacion;
import com.logistica.demo.logistica.compras.domain.CotizacionProveedor;
import com.logistica.demo.logistica.compras.domain.CotizacionProveedorDetalle;
import com.logistica.demo.logistica.compras.domain.EstadoCotizacionProveedor;
import com.logistica.demo.logistica.compras.domain.EstadoOrdenCompra;
import com.logistica.demo.logistica.compras.domain.EstadoProcesoCotizacion;
import com.logistica.demo.logistica.compras.domain.OrdenCompra;
import com.logistica.demo.logistica.compras.domain.OrdenCompraDetalle;
import com.logistica.demo.logistica.compras.dto.OrdenCompraDetalleResponse;
import com.logistica.demo.logistica.compras.dto.OrdenCompraResponse;
import com.logistica.demo.logistica.compras.repository.AdjudicacionRepository;
import com.logistica.demo.logistica.compras.repository.OrdenCompraRepository;
import com.logistica.demo.logistica.finanzas.service.DocumentTotals;
import com.logistica.demo.logistica.finanzas.service.FinancialCalculatorService;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.presupuesto.api.BudgetAllocation;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.CommitBudgetCommand;
import com.logistica.demo.logistica.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.web.PageResponse;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.sharedkernel.domain.DocumentReference;
import com.logistica.demo.sharedkernel.domain.FiscalDimension;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.idempotency.IdempotencyKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final RequerimientoRepository requerimientoRepository;
    private final AdjudicacionRepository adjudicacionRepository;
    private final FinancialCalculatorService financialCalculatorService;
    private final BudgetControlUseCase budgetControlUseCase;
    private final NeedsBalanceQuery needsBalanceQuery;
    private final CurrentUserService currentUserService;

    public OrdenCompraService(
            OrdenCompraRepository ordenCompraRepository,
            RequerimientoRepository requerimientoRepository,
            AdjudicacionRepository adjudicacionRepository,
            FinancialCalculatorService financialCalculatorService,
            BudgetControlUseCase budgetControlUseCase,
            NeedsBalanceQuery needsBalanceQuery,
            CurrentUserService currentUserService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.requerimientoRepository = requerimientoRepository;
        this.adjudicacionRepository = adjudicacionRepository;
        this.financialCalculatorService = financialCalculatorService;
        this.budgetControlUseCase = budgetControlUseCase;
        this.needsBalanceQuery = needsBalanceQuery;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public OrdenCompraResponse generarDesdeRequerimiento(Long requerimientoId) {
        Requerimiento requerimiento = requerimientoRepository.findById(requerimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento no encontrado."));

        if (requerimiento.getEstado() != EstadoRequerimiento.APROBADO) {
            throw new BusinessRuleException("Solo se puede generar una orden desde un requerimiento APROBADO.");
        }
        if (ordenCompraRepository.findByRequerimientoId(requerimientoId).isPresent()) {
            throw new BusinessRuleException("Ya existe una orden de compra para este requerimiento.");
        }
        requireBudgetPrecommit(requerimiento);

        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setRequerimiento(requerimiento);
        ordenCompra.setProveedor(requerimiento.getProveedor());
        ordenCompra.setEstado(EstadoOrdenCompra.GENERADA);
        ordenCompra.setBudgetControlId(requerimiento.getBudgetControlId());
        ordenCompra.setMoneda(requerimiento.getMoneda());
        ordenCompra.setTipoCambio(financialCalculatorService.resolveExchangeRate(requerimiento.getMoneda()));
        ordenCompra.setGeneratedAt(LocalDateTime.now());

        List<OrdenCompraDetalle> detalles = new ArrayList<>();
        for (RequerimientoDetalle detalleReq : requerimiento.getDetalles()) {
            OrdenCompraDetalle detalle = new OrdenCompraDetalle();
            detalle.setItem(detalleReq.getItem());
            detalle.setAlmacen(detalleReq.getAlmacen());
            detalle.setRequerimientoDetalle(detalleReq);
            detalle.setCantidad(detalleReq.getCantidad());
            detalle.setPrecioUnitario(detalleReq.getPrecioUnitarioEstimado());
            detalle.setSubtotalLinea(detalleReq.getSubtotalLinea());
            detalles.add(detalle);
        }
        detalles.forEach(ordenCompra::addDetalle);

        DocumentTotals totals = financialCalculatorService.calculateDocumentTotals(
                detalles.stream().map(OrdenCompraDetalle::getSubtotalLinea).toList(),
                requerimiento.getMoneda());
        ordenCompra.setSubtotal(totals.subtotal());
        ordenCompra.setIgv(totals.igv());
        ordenCompra.setTotal(totals.total());

        OrdenCompra saved = ordenCompraRepository.save(ordenCompra);
        saved.setNumero("OC-%06d".formatted(saved.getId()));

        requerimiento.cambiarEstado(
                EstadoRequerimiento.CONVERTIDO_OC,
                "Orden de compra %s generada.".formatted(saved.getNumero()));
        requerimiento.setOrdenCompra(saved);

        return mapResponse(saved);
    }

    @Transactional
    public OrdenCompraResponse generarDesdeAdjudicacion(Long adjudicacionId) {
        Adjudicacion adjudicacion = adjudicacionRepository.findById(adjudicacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjudicacion no encontrada."));
        CotizacionProveedor cotizacion = adjudicacion.getCotizacion();
        Requerimiento requerimiento = adjudicacion.getProceso().getRequerimiento();

        if (adjudicacion.getProceso().getEstado() != EstadoProcesoCotizacion.ADJUDICADO
                || cotizacion.getEstado() != EstadoCotizacionProveedor.ADJUDICADA) {
            throw new BusinessRuleException("Solo se puede generar OC desde una adjudicacion vigente.");
        }
        if (requerimiento.getEstado() != EstadoRequerimiento.APROBADO) {
            throw new BusinessRuleException("Solo se puede generar OC desde requerimientos APROBADOS.");
        }
        requireBudgetPrecommit(requerimiento);
        if (ordenCompraRepository.findByAdjudicacionId(adjudicacionId).isPresent()) {
            throw new BusinessRuleException("Ya existe una orden de compra para esta adjudicacion.");
        }
        if (ordenCompraRepository.findByRequerimientoId(requerimiento.getId()).isPresent()) {
            throw new BusinessRuleException("Ya existe una orden de compra para este requerimiento.");
        }

        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setRequerimiento(requerimiento);
        ordenCompra.setAdjudicacion(adjudicacion);
        ordenCompra.setProveedor(cotizacion.getProveedor());
        ordenCompra.setEstado(EstadoOrdenCompra.GENERADA);
        ordenCompra.setBudgetControlId(requerimiento.getBudgetControlId());
        ordenCompra.setMoneda(cotizacion.getMoneda());
        ordenCompra.setTipoCambio(financialCalculatorService.resolveExchangeRate(cotizacion.getMoneda()));
        ordenCompra.setGeneratedAt(LocalDateTime.now());

        List<OrdenCompraDetalle> detalles = new ArrayList<>();
        for (CotizacionProveedorDetalle detalleCotizacion : cotizacion.getDetalles()) {
            RequerimientoDetalle detalleReq = detalleCotizacion.getRequerimientoDetalle();
            OrdenCompraDetalle detalle = new OrdenCompraDetalle();
            detalle.setItem(detalleReq.getItem());
            detalle.setAlmacen(detalleReq.getAlmacen());
            detalle.setRequerimientoDetalle(detalleReq);
            detalle.setCantidad(detalleCotizacion.getCantidadOfertada());
            detalle.setPrecioUnitario(detalleCotizacion.getPrecioUnitario());
            detalle.setSubtotalLinea(detalleCotizacion.getSubtotalLinea());
            detalles.add(detalle);
        }
        detalles.forEach(ordenCompra::addDetalle);

        DocumentTotals totals = financialCalculatorService.calculateDocumentTotals(
                detalles.stream().map(OrdenCompraDetalle::getSubtotalLinea).toList(),
                cotizacion.getMoneda());
        ordenCompra.setSubtotal(totals.subtotal());
        ordenCompra.setIgv(totals.igv());
        ordenCompra.setTotal(totals.total());

        OrdenCompra saved = ordenCompraRepository.save(ordenCompra);
        saved.setNumero("OC-%06d".formatted(saved.getId()));

        requerimiento.cambiarEstado(
                EstadoRequerimiento.CONVERTIDO_OC,
                "Orden de compra %s generada desde adjudicacion %d.".formatted(saved.getNumero(), adjudicacionId));
        requerimiento.setOrdenCompra(saved);

        return mapResponse(saved);
    }

    @Transactional
    public OrdenCompraResponse aprobar(Long ordenCompraId) {
        OrdenCompra ordenCompra = ordenCompraRepository.findById(ordenCompraId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada."));

        if (ordenCompra.getEstado() == EstadoOrdenCompra.APROBADA) {
            return mapResponse(ordenCompra);
        }
        if (ordenCompra.getEstado() != EstadoOrdenCompra.GENERADA) {
            throw new BusinessRuleException("Solo se puede aprobar una orden de compra GENERADA.");
        }

        commitBudget(ordenCompra);

        ordenCompra.setEstado(EstadoOrdenCompra.APROBADA);
        ordenCompra.setApprovedAt(LocalDateTime.now());
        ordenCompra.setApprovedBy(currentUserService.getUsername());
        return mapResponse(ordenCompraRepository.save(ordenCompra));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrdenCompraResponse> listar(
            String numero,
            Long proveedorId,
            Moneda moneda,
            Long requerimientoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BadRequestException("fechaDesde no puede ser mayor que fechaHasta.");
        }
        LocalDateTime inicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fin = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;
        String numeroFiltro = numero == null || numero.isBlank() ? null : numero.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "generatedAt"));
        Page<OrdenCompraResponse> result = ordenCompraRepository
                .findAll(buildSearchSpecification(numeroFiltro, proveedorId, moneda, requerimientoId, inicio, fin), pageRequest)
                .map(this::mapResponse);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse getById(Long id) {
        return ordenCompraRepository.findById(id)
                .map(this::mapResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada."));
    }

    public OrdenCompraResponse mapResponse(OrdenCompra ordenCompra) {
        return new OrdenCompraResponse(
                ordenCompra.getId(),
                ordenCompra.getNumero(),
                ordenCompra.getRequerimiento().getId(),
                ordenCompra.getRequerimiento().getNumero(),
                ordenCompra.getAdjudicacion() != null ? ordenCompra.getAdjudicacion().getId() : null,
                new ProveedorResponse(
                        ordenCompra.getProveedor().getId(),
                        ordenCompra.getProveedor().getCode(),
                        ordenCompra.getProveedor().getName()),
                ordenCompra.getEstado(),
                ordenCompra.getBudgetControlId(),
                ordenCompra.getMoneda(),
                ordenCompra.getTipoCambio(),
                ordenCompra.getSubtotal(),
                ordenCompra.getIgv(),
                ordenCompra.getTotal(),
                ordenCompra.getGeneratedAt(),
                ordenCompra.getApprovedAt(),
                ordenCompra.getApprovedBy(),
                ordenCompra.getDetalles().stream()
                        .map(detalle -> new OrdenCompraDetalleResponse(
                                detalle.getId(),
                                detalle.getItem().getId(),
                                detalle.getItem().getCode(),
                                detalle.getItem().getName(),
                                detalle.getAlmacen().getId(),
                                detalle.getAlmacen().getCode(),
                                detalle.getAlmacen().getName(),
                                detalle.getCantidad(),
                                detalle.getCantidadRecibida(),
                                detalle.getCantidad() - detalle.getCantidadRecibida(),
                                detalle.getPrecioUnitario(),
                                detalle.getSubtotalLinea()))
                        .toList(),
                ordenCompra.getCreatedBy(),
                ordenCompra.getCreatedAt());
    }

    private Specification<OrdenCompra> buildSearchSpecification(
            String numero,
            Long proveedorId,
            Moneda moneda,
            Long requerimientoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (numero != null) {
                predicates.add(cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%"));
            }
            if (proveedorId != null) {
                predicates.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }
            if (moneda != null) {
                predicates.add(cb.equal(root.get("moneda"), moneda));
            }
            if (requerimientoId != null) {
                predicates.add(cb.equal(root.get("requerimiento").get("id"), requerimientoId));
            }
            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("generatedAt"), fechaInicio));
            }
            if (fechaFin != null) {
                predicates.add(cb.lessThan(root.get("generatedAt"), fechaFin));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void commitBudget(OrdenCompra ordenCompra) {
        Long budgetControlId = ordenCompra.getBudgetControlId() != null
                ? ordenCompra.getBudgetControlId()
                : ordenCompra.getRequerimiento().getBudgetControlId();
        if (budgetControlId == null) {
            throw new BusinessRuleException("La orden de compra no tiene control presupuestal para comprometer.");
        }

        List<BudgetAllocation> allocations = buildBudgetAllocations(ordenCompra);
        BudgetControlResult result = budgetControlUseCase.commit(new CommitBudgetCommand(
                budgetControlId,
                new DocumentReference(
                        "LOGISTICA",
                        "ORDEN_COMPRA",
                        ordenCompra.getId(),
                        ordenCompra.getNumero()),
                allocations,
                new IdempotencyKey("logistica-orden-compra-commit-" + ordenCompra.getId()),
                currentUserService.getUsername()));
        ordenCompra.setBudgetControlId(result.budgetControlId());
    }

    private void requireBudgetPrecommit(Requerimiento requerimiento) {
        if (requerimiento.getBudgetControlId() == null) {
            throw new BusinessRuleException(
                    "El requerimiento no tiene precompromiso presupuestal. No se puede iniciar compra sin presupuesto reservado.");
        }
    }

    private List<BudgetAllocation> buildBudgetAllocations(OrdenCompra ordenCompra) {
        Requerimiento requerimiento = ordenCompra.getRequerimiento();
        if (requerimiento.getCompanyId() == null || requerimiento.getFiscalYear() == null) {
            throw new BusinessRuleException("El requerimiento no tiene trazabilidad presupuestal completa.");
        }

        List<BudgetAllocation> allocations = new ArrayList<>();
        for (OrdenCompraDetalle detalle : ordenCompra.getDetalles()) {
            RequerimientoDetalle requerimientoDetalle = detalle.getRequerimientoDetalle();
            if (requerimientoDetalle == null || requerimientoDetalle.getNeedsLineId() == null) {
                throw new BusinessRuleException("Todos los detalles de la OC deben provenir de una linea de Cuadro.");
            }
            NeedsLineBalance balance = needsBalanceQuery
                    .findAvailableLine(requerimiento.getCompanyId(), requerimientoDetalle.getNeedsLineId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "La linea de Cuadro no tiene saldo disponible para comprometer."));
            allocations.addAll(splitDetailAcrossMonths(ordenCompra, detalle, balance));
        }
        if (allocations.isEmpty()) {
            throw new BusinessRuleException("La orden de compra no tiene importes para comprometer.");
        }
        return allocations;
    }

    private List<BudgetAllocation> splitDetailAcrossMonths(
            OrdenCompra ordenCompra,
            OrdenCompraDetalle detalle,
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
                    : detalle.getPrecioUnitario().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
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
                    new Money(amount, ordenCompra.getMoneda())));
        }

        if (remainingQuantity.signum() > 0) {
            throw new BusinessRuleException(
                    "La cantidad adjudicada supera el saldo mensual disponible de la linea de Cuadro.");
        }
        return allocations;
    }
}
