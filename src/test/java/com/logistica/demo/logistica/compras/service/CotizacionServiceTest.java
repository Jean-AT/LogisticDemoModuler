package com.logistica.demo.logistica.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.logistica.demo.logistica.compras.domain.CotizacionProveedor;
import com.logistica.demo.logistica.compras.domain.EstadoProcesoCotizacion;
import com.logistica.demo.logistica.compras.domain.ProcesoCotizacion;
import com.logistica.demo.logistica.compras.dto.AdjudicacionRequest;
import com.logistica.demo.logistica.compras.dto.CotizacionLineaRequest;
import com.logistica.demo.logistica.compras.dto.CotizacionProveedorRequest;
import com.logistica.demo.logistica.compras.repository.CotizacionProveedorRepository;
import com.logistica.demo.logistica.compras.repository.ProcesoCotizacionRepository;
import com.logistica.demo.logistica.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.maestros.repository.ProveedorRepository;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CotizacionServiceTest {

    private ProcesoCotizacionRepository procesoRepository;
    private CotizacionProveedorRepository cotizacionRepository;
    private RequerimientoRepository requerimientoRepository;
    private ProveedorRepository proveedorRepository;
    private CurrentUserService currentUserService;
    private CotizacionService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoCotizacionRepository.class);
        cotizacionRepository = mock(CotizacionProveedorRepository.class);
        requerimientoRepository = mock(RequerimientoRepository.class);
        proveedorRepository = mock(ProveedorRepository.class);
        currentUserService = mock(CurrentUserService.class);
        service = new CotizacionService(
                procesoRepository,
                cotizacionRepository,
                requerimientoRepository,
                proveedorRepository,
                currentUserService);
    }

    @Test
    void shouldQuoteCloseAndAwardARequirement() {
        Requerimiento requerimiento = requerimiento();
        Proveedor proveedor = proveedor(20L);
        when(requerimientoRepository.findById(10L)).thenReturn(Optional.of(requerimiento));
        when(procesoRepository.existsByRequerimientoId(10L)).thenReturn(false);
        when(procesoRepository.saveAndFlush(any(ProcesoCotizacion.class))).thenAnswer(invocation -> assignIds(invocation.getArgument(0)));
        when(procesoRepository.findById(100L)).thenAnswer(invocation -> Optional.of(openProcess(requerimiento)));
        when(proveedorRepository.findById(20L)).thenReturn(Optional.of(proveedor));
        when(cotizacionRepository.findByProcesoIdAndProveedorId(100L, 20L)).thenReturn(Optional.empty());
        when(currentUserService.getUsername()).thenReturn("compras");

        var opened = service.abrirDesdeRequerimiento(10L);
        assertEquals(EstadoProcesoCotizacion.ABIERTO, opened.estado());

        var quoted = service.registrarCotizacion(100L, new CotizacionProveedorRequest(
                20L,
                Moneda.PEN,
                java.util.List.of(new CotizacionLineaRequest(500L, 3, new BigDecimal("9.50")))));
        assertEquals(1, quoted.cotizaciones().size());
        assertEquals(new BigDecimal("28.50"), quoted.cotizaciones().get(0).total());

        ProcesoCotizacion closedProcess = openProcess(requerimiento);
        CotizacionProveedor quote = quote(closedProcess, proveedor);
        closedProcess.addCotizacion(quote);
        when(procesoRepository.findById(101L)).thenReturn(Optional.of(closedProcess));

        var closed = service.cerrar(101L);
        assertEquals(EstadoProcesoCotizacion.CERRADO, closed.estado());

        closedProcess.setId(102L);
        closedProcess.setEstado(EstadoProcesoCotizacion.CERRADO);
        quote.setId(200L);
        when(procesoRepository.findById(102L)).thenReturn(Optional.of(closedProcess));

        var awarded = service.adjudicar(102L, new AdjudicacionRequest(200L));
        assertEquals(EstadoProcesoCotizacion.ADJUDICADO, awarded.estado());
        assertEquals(200L, awarded.adjudicacion().cotizacionId());
        assertEquals("compras", awarded.adjudicacion().actor());
    }

    @Test
    void shouldRejectAwardWhenProcessIsStillOpen() {
        Requerimiento requerimiento = requerimiento();
        ProcesoCotizacion process = openProcess(requerimiento);
        process.addCotizacion(quote(process, proveedor(20L)));
        when(procesoRepository.findById(100L)).thenReturn(Optional.of(process));

        assertThrows(BusinessRuleException.class, () -> service.adjudicar(100L, new AdjudicacionRequest(200L)));
    }

    private ProcesoCotizacion assignIds(ProcesoCotizacion process) {
        if (process.getId() == null) {
            process.setId(100L);
        }
        long nextQuoteId = 200L;
        long nextLineId = 300L;
        for (CotizacionProveedor quote : process.getCotizaciones()) {
            quote.setId(nextQuoteId++);
            for (var line : quote.getDetalles()) {
                line.setId(nextLineId++);
            }
        }
        if (process.getAdjudicacion() != null) {
            process.getAdjudicacion().setId(400L);
        }
        return process;
    }

    private ProcesoCotizacion openProcess(Requerimiento requerimiento) {
        ProcesoCotizacion process = new ProcesoCotizacion();
        process.setId(100L);
        process.setRequerimiento(requerimiento);
        process.setEstado(EstadoProcesoCotizacion.ABIERTO);
        process.setOpenedAt(java.time.LocalDateTime.now());
        return process;
    }

    private CotizacionProveedor quote(ProcesoCotizacion process, Proveedor proveedor) {
        CotizacionProveedor quote = new CotizacionProveedor();
        quote.setId(200L);
        quote.setProceso(process);
        quote.setProveedor(proveedor);
        quote.setMoneda(Moneda.PEN);
        quote.setEstado(com.logistica.demo.logistica.compras.domain.EstadoCotizacionProveedor.PRESENTADA);
        quote.setSubmittedAt(java.time.LocalDateTime.now());
        quote.setTotal(new BigDecimal("28.50"));
        return quote;
    }

    private Requerimiento requerimiento() {
        Proveedor proveedor = proveedor(11L);
        Item item = new Item();
        item.setId(70L);
        item.setCode("SERV-001");
        item.setName("Servicio demo");
        Almacen almacen = new Almacen();
        almacen.setId(80L);
        almacen.setCode("ALM-001");
        almacen.setName("Almacen demo");

        RequerimientoDetalle detalle = new RequerimientoDetalle();
        detalle.setId(500L);
        detalle.setItem(item);
        detalle.setAlmacen(almacen);
        detalle.setCantidad(3);
        detalle.setPrecioUnitarioEstimado(new BigDecimal("10.00"));
        detalle.setSubtotalLinea(new BigDecimal("30.00"));

        Requerimiento requerimiento = new Requerimiento();
        requerimiento.setId(10L);
        requerimiento.setNumero("REQ-000010");
        requerimiento.setDescripcion("Req aprobado");
        requerimiento.setProveedor(proveedor);
        requerimiento.setMoneda(Moneda.PEN);
        requerimiento.setEstado(EstadoRequerimiento.APROBADO);
        requerimiento.addDetalle(detalle);
        return requerimiento;
    }

    private Proveedor proveedor(Long id) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setCode("PRV-%03d".formatted(id));
        proveedor.setName("Proveedor " + id);
        proveedor.setActive(true);
        return proveedor;
    }
}
