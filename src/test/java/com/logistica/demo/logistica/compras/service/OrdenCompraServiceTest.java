package com.logistica.demo.logistica.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.logistica.demo.logistica.compras.domain.ProcesoCotizacion;
import com.logistica.demo.logistica.compras.repository.AdjudicacionRepository;
import com.logistica.demo.logistica.compras.repository.OrdenCompraRepository;
import com.logistica.demo.logistica.finanzas.service.DocumentTotals;
import com.logistica.demo.logistica.finanzas.service.FinancialCalculatorService;
import com.logistica.demo.logistica.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlStatus;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.CommitBudgetCommand;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrdenCompraServiceTest {

    private OrdenCompraRepository ordenCompraRepository;
    private RequerimientoRepository requerimientoRepository;
    private AdjudicacionRepository adjudicacionRepository;
    private FinancialCalculatorService financialCalculatorService;
    private BudgetControlUseCase budgetControlUseCase;
    private NeedsBalanceQuery needsBalanceQuery;
    private CurrentUserService currentUserService;
    private OrdenCompraService service;

    @BeforeEach
    void setUp() {
        ordenCompraRepository = mock(OrdenCompraRepository.class);
        requerimientoRepository = mock(RequerimientoRepository.class);
        adjudicacionRepository = mock(AdjudicacionRepository.class);
        financialCalculatorService = mock(FinancialCalculatorService.class);
        budgetControlUseCase = mock(BudgetControlUseCase.class);
        needsBalanceQuery = mock(NeedsBalanceQuery.class);
        currentUserService = mock(CurrentUserService.class);
        service = new OrdenCompraService(
                ordenCompraRepository,
                requerimientoRepository,
                adjudicacionRepository,
                financialCalculatorService,
                budgetControlUseCase,
                needsBalanceQuery,
                currentUserService);
    }

    @Test
    void shouldGeneratePurchaseOrderFromAwardedQuote() {
        Adjudicacion adjudicacion = adjudicacion();
        when(adjudicacionRepository.findById(400L)).thenReturn(Optional.of(adjudicacion));
        when(ordenCompraRepository.findByAdjudicacionId(400L)).thenReturn(Optional.empty());
        when(ordenCompraRepository.findByRequerimientoId(90L)).thenReturn(Optional.empty());
        when(financialCalculatorService.resolveExchangeRate(Moneda.PEN)).thenReturn(BigDecimal.ONE);
        when(financialCalculatorService.calculateDocumentTotals(List.of(new BigDecimal("28.50")), Moneda.PEN))
                .thenReturn(new DocumentTotals(new BigDecimal("28.50"), new BigDecimal("5.13"), new BigDecimal("33.63"), BigDecimal.ONE));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(invocation -> {
            OrdenCompra ordenCompra = invocation.getArgument(0);
            ordenCompra.setId(600L);
            return ordenCompra;
        });

        var response = service.generarDesdeAdjudicacion(400L);

        assertEquals(600L, response.id());
        assertEquals("OC-000600", response.numero());
        assertEquals(400L, response.adjudicacionId());
        assertEquals(EstadoOrdenCompra.GENERADA, response.estado());
        assertEquals(700L, response.budgetControlId());
        assertEquals(20L, response.proveedor().id());
        assertEquals(new BigDecimal("28.50"), response.subtotal());
        assertEquals(new BigDecimal("9.50"), response.detalles().get(0).precioUnitario());
        assertEquals(EstadoRequerimiento.CONVERTIDO_OC, adjudicacion.getProceso().getRequerimiento().getEstado());
    }

    @Test
    void shouldApprovePurchaseOrderAndCommitBudget() {
        OrdenCompra ordenCompra = ordenCompra();
        when(ordenCompraRepository.findById(600L)).thenReturn(Optional.of(ordenCompra));
        when(needsBalanceQuery.findAvailableLine(1L, 22L)).thenReturn(Optional.of(needsBalance()));
        when(currentUserService.getUsername()).thenReturn("aprobador");
        when(budgetControlUseCase.commit(any(CommitBudgetCommand.class))).thenReturn(new BudgetControlResult(
                700L,
                BudgetControlStatus.COMMITTED,
                new Money(new BigDecimal("28.50"), Moneda.PEN),
                new Money(new BigDecimal("971.50"), Moneda.PEN)));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.aprobar(600L);

        ArgumentCaptor<CommitBudgetCommand> captor = ArgumentCaptor.forClass(CommitBudgetCommand.class);
        verify(budgetControlUseCase).commit(captor.capture());
        CommitBudgetCommand command = captor.getValue();
        assertEquals(700L, command.budgetControlId());
        assertEquals("LOGISTICA", command.source().module());
        assertEquals("ORDEN_COMPRA", command.source().type());
        assertEquals(600L, command.source().id());
        assertEquals("OC-000600", command.source().number());
        assertEquals("logistica-orden-compra-commit-600", command.idempotencyKey().value());
        assertEquals("aprobador", command.actor());
        assertEquals(2, command.allocations().size());
        assertEquals(new BigDecimal("19.00"), command.allocations().get(0).amount().amount());
        assertEquals(new BigDecimal("9.50"), command.allocations().get(1).amount().amount());
        assertEquals(EstadoOrdenCompra.APROBADA, response.estado());
        assertEquals("aprobador", response.approvedBy());
    }

    @Test
    void shouldRejectPurchaseOrderGenerationWhenRequirementHasNoBudgetPrecommit() {
        Requerimiento requerimiento = requerimiento();
        requerimiento.setBudgetControlId(null);
        when(requerimientoRepository.findById(90L)).thenReturn(Optional.of(requerimiento));
        when(ordenCompraRepository.findByRequerimientoId(90L)).thenReturn(Optional.empty());

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.generarDesdeRequerimiento(90L));

        assertEquals(
                "El requerimiento no tiene precompromiso presupuestal. No se puede iniciar compra sin presupuesto reservado.",
                exception.getMessage());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void shouldRejectAwardedPurchaseOrderGenerationWhenRequirementHasNoBudgetPrecommit() {
        Adjudicacion adjudicacion = adjudicacion();
        adjudicacion.getProceso().getRequerimiento().setBudgetControlId(null);
        when(adjudicacionRepository.findById(400L)).thenReturn(Optional.of(adjudicacion));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.generarDesdeAdjudicacion(400L));

        assertEquals(
                "El requerimiento no tiene precompromiso presupuestal. No se puede iniciar compra sin presupuesto reservado.",
                exception.getMessage());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void shouldRejectPurchaseOrderApprovalWhenBudgetControlIsMissing() {
        OrdenCompra ordenCompra = ordenCompra();
        ordenCompra.setBudgetControlId(null);
        ordenCompra.getRequerimiento().setBudgetControlId(null);
        when(ordenCompraRepository.findById(600L)).thenReturn(Optional.of(ordenCompra));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.aprobar(600L));

        assertEquals("La orden de compra no tiene control presupuestal para comprometer.", exception.getMessage());
        verify(budgetControlUseCase, never()).commit(any());
    }

    private Adjudicacion adjudicacion() {
        Requerimiento requerimiento = requerimiento();
        ProcesoCotizacion process = new ProcesoCotizacion();
        process.setId(100L);
        process.setRequerimiento(requerimiento);
        process.setEstado(EstadoProcesoCotizacion.ADJUDICADO);
        process.setOpenedAt(LocalDateTime.now());

        CotizacionProveedor quote = new CotizacionProveedor();
        quote.setId(200L);
        quote.setProceso(process);
        quote.setProveedor(proveedor(20L));
        quote.setMoneda(Moneda.PEN);
        quote.setEstado(EstadoCotizacionProveedor.ADJUDICADA);
        quote.setSubmittedAt(LocalDateTime.now());
        quote.setTotal(new BigDecimal("28.50"));

        CotizacionProveedorDetalle quoteLine = new CotizacionProveedorDetalle();
        quoteLine.setId(300L);
        quoteLine.setRequerimientoDetalle(requerimiento.getDetalles().get(0));
        quoteLine.setCantidadOfertada(3);
        quoteLine.setPrecioUnitario(new BigDecimal("9.50"));
        quoteLine.setSubtotalLinea(new BigDecimal("28.50"));
        quote.addDetalle(quoteLine);

        Adjudicacion adjudicacion = new Adjudicacion();
        adjudicacion.setId(400L);
        adjudicacion.setProceso(process);
        adjudicacion.setCotizacion(quote);
        adjudicacion.setAwardedAt(LocalDateTime.now());
        adjudicacion.setActor("compras");
        process.setAdjudicacion(adjudicacion);
        quote.setAdjudicacion(adjudicacion);
        return adjudicacion;
    }

    private OrdenCompra ordenCompra() {
        Requerimiento requerimiento = requerimiento();
        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setId(600L);
        ordenCompra.setNumero("OC-000600");
        ordenCompra.setRequerimiento(requerimiento);
        ordenCompra.setProveedor(proveedor(20L));
        ordenCompra.setEstado(EstadoOrdenCompra.GENERADA);
        ordenCompra.setBudgetControlId(700L);
        ordenCompra.setMoneda(Moneda.PEN);
        ordenCompra.setTipoCambio(BigDecimal.ONE);
        ordenCompra.setGeneratedAt(LocalDateTime.now());
        ordenCompra.setSubtotal(new BigDecimal("28.50"));
        ordenCompra.setIgv(new BigDecimal("5.13"));
        ordenCompra.setTotal(new BigDecimal("33.63"));

        OrdenCompraDetalle detalle = new OrdenCompraDetalle();
        detalle.setId(610L);
        detalle.setItem(requerimiento.getDetalles().get(0).getItem());
        detalle.setAlmacen(requerimiento.getDetalles().get(0).getAlmacen());
        detalle.setRequerimientoDetalle(requerimiento.getDetalles().get(0));
        detalle.setCantidad(3);
        detalle.setPrecioUnitario(new BigDecimal("9.50"));
        detalle.setSubtotalLinea(new BigDecimal("28.50"));
        ordenCompra.addDetalle(detalle);
        return ordenCompra;
    }

    private Requerimiento requerimiento() {
        RequerimientoDetalle detalle = new RequerimientoDetalle();
        detalle.setId(500L);
        detalle.setItem(item());
        detalle.setAlmacen(almacen());
        detalle.setCantidad(3);
        detalle.setNeedsLineId(22L);
        detalle.setPrecioUnitarioEstimado(new BigDecimal("10.00"));
        detalle.setSubtotalLinea(new BigDecimal("30.00"));

        Requerimiento requerimiento = new Requerimiento();
        requerimiento.setId(90L);
        requerimiento.setNumero("REQ-000090");
        requerimiento.setDescripcion("Req adjudicado");
        requerimiento.setProveedor(proveedor(11L));
        requerimiento.setMoneda(Moneda.PEN);
        requerimiento.setEstado(EstadoRequerimiento.APROBADO);
        requerimiento.setCompanyId(1L);
        requerimiento.setFiscalYear(2026);
        requerimiento.setNeedsPlanId(44L);
        requerimiento.setNeedsLineId(22L);
        requerimiento.setBudgetControlId(700L);
        requerimiento.addDetalle(detalle);
        return requerimiento;
    }

    private NeedsLineBalance needsBalance() {
        return new NeedsLineBalance(
                44L,
                22L,
                1L,
                2026,
                101L,
                102L,
                103L,
                104L,
                55L,
                new BigDecimal("10.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("8.0000"),
                List.of(
                        new MonthlyNeedBalance(
                                1,
                                new BigDecimal("2.0000"),
                                BigDecimal.ZERO,
                                new BigDecimal("2.0000")),
                        new MonthlyNeedBalance(
                                2,
                                new BigDecimal("6.0000"),
                                BigDecimal.ZERO,
                                new BigDecimal("6.0000"))));
    }

    private Proveedor proveedor(Long id) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setCode("PRV-%03d".formatted(id));
        proveedor.setName("Proveedor " + id);
        proveedor.setActive(true);
        return proveedor;
    }

    private Item item() {
        Item item = new Item();
        item.setId(77L);
        item.setCode("SERV-001");
        item.setName("Servicio Demo");
        item.setUnitMeasure("UND");
        item.setActive(true);
        return item;
    }

    private Almacen almacen() {
        Almacen almacen = new Almacen();
        almacen.setId(33L);
        almacen.setCode("ALM-001");
        almacen.setName("Almacen Demo");
        almacen.setActive(true);
        return almacen;
    }
}
