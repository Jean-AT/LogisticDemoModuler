package com.logistica.demo.logistica.requerimientos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistica.demo.cuadronecesidades.api.NeedsBalanceQuery;
import com.logistica.demo.cuadronecesidades.api.NeedsLineBalance;
import com.logistica.demo.logistica.finanzas.service.FinancialCalculatorService;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.dto.RequerimientoFromNeedsLineRequest;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.maestros.repository.AlmacenRepository;
import com.logistica.demo.maestros.repository.ItemRepository;
import com.logistica.demo.maestros.repository.ProveedorRepository;
import com.logistica.demo.platform.api.MasterDataReference;
import com.logistica.demo.platform.api.PlatformCatalogQuery;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequerimientoServiceTest {

    private RequerimientoRepository requerimientoRepository;
    private ProveedorRepository proveedorRepository;
    private ItemRepository itemRepository;
    private AlmacenRepository almacenRepository;
    private FinancialCalculatorService financialCalculatorService;
    private NeedsBalanceQuery needsBalanceQuery;
    private PlatformCatalogQuery platformCatalogQuery;
    private RequerimientoService service;

    @BeforeEach
    void setUp() {
        requerimientoRepository = mock(RequerimientoRepository.class);
        proveedorRepository = mock(ProveedorRepository.class);
        itemRepository = mock(ItemRepository.class);
        almacenRepository = mock(AlmacenRepository.class);
        financialCalculatorService = mock(FinancialCalculatorService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        needsBalanceQuery = mock(NeedsBalanceQuery.class);
        platformCatalogQuery = mock(PlatformCatalogQuery.class);

        service = new RequerimientoService(
                requerimientoRepository,
                proveedorRepository,
                itemRepository,
                almacenRepository,
                financialCalculatorService,
                currentUserService,
                needsBalanceQuery,
                platformCatalogQuery);
    }

    @Test
    void createFromNeedsLineCopiesTraceabilityAndControlsAvailableQuantity() {
        var request = new RequerimientoFromNeedsLineRequest(
                "Compra desde Cuadro",
                11L,
                Moneda.PEN,
                1L,
                22L,
                33L,
                4,
                new BigDecimal("12.50"));
        NeedsLineBalance balance = new NeedsLineBalance(
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
                List.of());
        when(needsBalanceQuery.findAvailableLine(1L, 22L)).thenReturn(Optional.of(balance));
        when(platformCatalogQuery.findActiveCatalogItem(55L))
                .thenReturn(Optional.of(new MasterDataReference(55L, "SERV-001", "Servicio", true)));

        Proveedor proveedor = proveedor(11L);
        Item item = item(77L, "SERV-001");
        Almacen almacen = almacen(33L);
        when(proveedorRepository.findById(11L)).thenReturn(Optional.of(proveedor));
        when(itemRepository.findByCodeIgnoreCaseAndActiveTrue("SERV-001")).thenReturn(Optional.of(item));
        when(almacenRepository.findById(33L)).thenReturn(Optional.of(almacen));
        when(financialCalculatorService.calculateLineSubtotal(4, new BigDecimal("12.50")))
                .thenReturn(new BigDecimal("50.00"));
        when(requerimientoRepository.saveAndFlush(any(Requerimiento.class))).thenAnswer(invocation -> {
            Requerimiento saved = invocation.getArgument(0);
            saved.setId(90L);
            return saved;
        });

        var response = service.createFromNeedsLine(request);

        assertEquals("REQ-000090", response.numero());
        assertEquals(1L, response.companyId());
        assertEquals(2026, response.fiscalYear());
        assertEquals(44L, response.needsPlanId());
        assertEquals(22L, response.needsLineId());
        assertEquals(1, response.detalles().size());
        assertEquals(22L, response.detalles().get(0).needsLineId());
        assertEquals(new BigDecimal("8.0000"), response.detalles().get(0).availableQuantitySnapshot());
        assertEquals(77L, response.detalles().get(0).itemId());
    }

    @Test
    void createFromNeedsLineRejectsQuantityGreaterThanAvailable() {
        var request = new RequerimientoFromNeedsLineRequest(
                "Compra desde Cuadro",
                11L,
                Moneda.PEN,
                1L,
                22L,
                33L,
                9,
                new BigDecimal("12.50"));
        NeedsLineBalance balance = new NeedsLineBalance(
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
                List.of());
        when(needsBalanceQuery.findAvailableLine(1L, 22L)).thenReturn(Optional.of(balance));

        assertThrows(BusinessRuleException.class, () -> service.createFromNeedsLine(request));
        verify(requerimientoRepository, never()).saveAndFlush(any());
    }

    private Proveedor proveedor(Long id) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setCode("PRV-001");
        proveedor.setName("Proveedor Demo");
        proveedor.setActive(true);
        return proveedor;
    }

    private Item item(Long id, String code) {
        Item item = new Item();
        item.setId(id);
        item.setCode(code);
        item.setName("Item Demo");
        item.setUnitMeasure("UND");
        item.setActive(true);
        return item;
    }

    private Almacen almacen(Long id) {
        Almacen almacen = new Almacen();
        almacen.setId(id);
        almacen.setCode("ALM-001");
        almacen.setName("Almacen Demo");
        almacen.setActive(true);
        return almacen;
    }
}
