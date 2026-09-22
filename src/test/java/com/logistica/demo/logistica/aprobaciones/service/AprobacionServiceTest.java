package com.logistica.demo.logistica.aprobaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistica.demo.cuadronecesidades.api.MonthlyNeedBalance;
import com.logistica.demo.cuadronecesidades.api.NeedsBalanceQuery;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import com.logistica.demo.logistica.aprobaciones.dto.AprobacionDecisionRequest;
import com.logistica.demo.logistica.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.logistica.requerimientos.service.RequerimientoService;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.presupuesto.api.BudgetControlResult;
import com.logistica.demo.presupuesto.api.BudgetControlStatus;
import com.logistica.demo.presupuesto.api.BudgetControlUseCase;
import com.logistica.demo.presupuesto.api.PrecommitBudgetCommand;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.Money;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AprobacionServiceTest {

    private RequerimientoRepository requerimientoRepository;
    private RequerimientoService requerimientoService;
    private BudgetControlUseCase budgetControlUseCase;
    private NeedsBalanceQuery needsBalanceQuery;
    private CurrentUserService currentUserService;
    private AprobacionService service;

    @BeforeEach
    void setUp() {
        requerimientoRepository = mock(RequerimientoRepository.class);
        requerimientoService = mock(RequerimientoService.class);
        budgetControlUseCase = mock(BudgetControlUseCase.class);
        needsBalanceQuery = mock(NeedsBalanceQuery.class);
        currentUserService = mock(CurrentUserService.class);
        service = new AprobacionService(
                requerimientoRepository,
                requerimientoService,
                budgetControlUseCase,
                needsBalanceQuery,
                currentUserService);
    }

    @Test
    void approveRequirementFromNeedsLineCreatesBudgetPrecommit() {
        Requerimiento requerimiento = requerimientoFromNeeds();
        when(requerimientoRepository.findById(90L)).thenReturn(Optional.of(requerimiento));
        when(needsBalanceQuery.findAvailableLine(1L, 22L)).thenReturn(Optional.of(needsBalance()));
        when(currentUserService.getUsername()).thenReturn("aprobador");
        when(budgetControlUseCase.precommit(any(PrecommitBudgetCommand.class))).thenReturn(new BudgetControlResult(
                700L,
                BudgetControlStatus.PRECOMMITTED,
                new Money(new BigDecimal("40.00"), Moneda.PEN),
                new Money(new BigDecimal("960.00"), Moneda.PEN)));
        when(requerimientoRepository.saveAndFlush(any(Requerimiento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.aprobar(90L, new AprobacionDecisionRequest("ok"));

        ArgumentCaptor<PrecommitBudgetCommand> captor = ArgumentCaptor.forClass(PrecommitBudgetCommand.class);
        verify(budgetControlUseCase).precommit(captor.capture());
        PrecommitBudgetCommand command = captor.getValue();
        assertEquals("LOGISTICA", command.source().module());
        assertEquals("REQUERIMIENTO", command.source().type());
        assertEquals(90L, command.source().id());
        assertEquals("REQ-000090", command.source().number());
        assertEquals("logistica-requerimiento-precommit-90", command.idempotencyKey().value());
        assertEquals("aprobador", command.actor());
        assertEquals(2, command.allocations().size());
        assertEquals(1, command.allocations().get(0).dimension().month());
        assertEquals(new BigDecimal("20.00"), command.allocations().get(0).amount().amount());
        assertEquals(2, command.allocations().get(1).dimension().month());
        assertEquals(new BigDecimal("20.00"), command.allocations().get(1).amount().amount());
        assertEquals(EstadoRequerimiento.APROBADO, requerimiento.getEstado());
        assertEquals(700L, requerimiento.getBudgetControlId());
    }

    @Test
    void observeRequirementDoesNotPrecommitBudget() {
        Requerimiento requerimiento = requerimientoFromNeeds();
        when(requerimientoRepository.findById(90L)).thenReturn(Optional.of(requerimiento));
        when(requerimientoRepository.saveAndFlush(any(Requerimiento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.observar(90L, new AprobacionDecisionRequest("corregir"));

        verify(budgetControlUseCase, never()).precommit(any());
        assertEquals(EstadoRequerimiento.OBSERVADO, requerimiento.getEstado());
    }

    private Requerimiento requerimientoFromNeeds() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(11L);
        proveedor.setCode("PRV-001");
        proveedor.setName("Proveedor Demo");

        Item item = new Item();
        item.setId(77L);
        item.setCode("SERV-001");
        item.setName("Servicio Demo");

        Almacen almacen = new Almacen();
        almacen.setId(33L);
        almacen.setCode("ALM-001");
        almacen.setName("Almacen Demo");

        RequerimientoDetalle detalle = new RequerimientoDetalle();
        detalle.setItem(item);
        detalle.setAlmacen(almacen);
        detalle.setCantidad(4);
        detalle.setNeedsLineId(22L);
        detalle.setPrecioUnitarioEstimado(new BigDecimal("10.00"));
        detalle.setSubtotalLinea(new BigDecimal("40.00"));

        Requerimiento requerimiento = new Requerimiento();
        requerimiento.setId(90L);
        requerimiento.setNumero("REQ-000090");
        requerimiento.setDescripcion("Compra desde Cuadro");
        requerimiento.setProveedor(proveedor);
        requerimiento.setMoneda(Moneda.PEN);
        requerimiento.setEstado(EstadoRequerimiento.ENVIADO);
        requerimiento.setCompanyId(1L);
        requerimiento.setFiscalYear(2026);
        requerimiento.setNeedsPlanId(44L);
        requerimiento.setNeedsLineId(22L);
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
}
