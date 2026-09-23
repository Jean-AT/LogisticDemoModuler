package com.logistica.demo.logistica.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.logistica.demo.logistica.compras.domain.EstadoOrdenCompra;
import com.logistica.demo.logistica.compras.domain.EstadoRecepcionAlmacen;
import com.logistica.demo.logistica.compras.domain.OrdenCompra;
import com.logistica.demo.logistica.compras.domain.OrdenCompraDetalle;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacen;
import com.logistica.demo.logistica.compras.dto.RecepcionAlmacenRequest;
import com.logistica.demo.logistica.compras.dto.RecepcionLineaRequest;
import com.logistica.demo.logistica.compras.dto.RecepcionReversionRequest;
import com.logistica.demo.logistica.compras.repository.OrdenCompraRepository;
import com.logistica.demo.logistica.compras.repository.RecepcionAlmacenRepository;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecepcionAlmacenServiceTest {

    private OrdenCompraRepository ordenCompraRepository;
    private RecepcionAlmacenRepository recepcionRepository;
    private CurrentUserService currentUserService;
    private RecepcionAlmacenService service;

    @BeforeEach
    void setUp() {
        ordenCompraRepository = mock(OrdenCompraRepository.class);
        recepcionRepository = mock(RecepcionAlmacenRepository.class);
        currentUserService = mock(CurrentUserService.class);
        service = new RecepcionAlmacenService(ordenCompraRepository, recepcionRepository, currentUserService);
    }

    @Test
    void shouldRegisterPartialReceiptAndUpdatePurchaseOrderStatus() {
        OrdenCompra ordenCompra = ordenCompra();
        when(ordenCompraRepository.findById(600L)).thenReturn(Optional.of(ordenCompra));
        when(currentUserService.getUsername()).thenReturn("compras");
        when(recepcionRepository.save(any(RecepcionAlmacen.class))).thenAnswer(invocation -> {
            RecepcionAlmacen recepcion = invocation.getArgument(0);
            recepcion.setId(900L);
            long lineId = 910L;
            for (var detalle : recepcion.getDetalles()) {
                detalle.setId(lineId++);
            }
            return recepcion;
        });

        var response = service.registrar(600L, new RecepcionAlmacenRequest(List.of(
                new RecepcionLineaRequest(610L, 2))));

        assertEquals(900L, response.id());
        assertEquals("REC-000900", response.numero());
        assertEquals(EstadoRecepcionAlmacen.REGISTRADA, response.estado());
        assertEquals("compras", response.actor());
        assertEquals(2, ordenCompra.getDetalles().get(0).getCantidadRecibida());
        assertEquals(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA, ordenCompra.getEstado());
    }

    @Test
    void shouldRejectReceiptOverPendingQuantity() {
        OrdenCompra ordenCompra = ordenCompra();
        ordenCompra.getDetalles().get(0).setCantidadRecibida(2);
        when(ordenCompraRepository.findById(600L)).thenReturn(Optional.of(ordenCompra));

        assertThrows(BusinessRuleException.class, () -> service.registrar(600L, new RecepcionAlmacenRequest(List.of(
                new RecepcionLineaRequest(610L, 2)))));
    }

    @Test
    void shouldReverseReceiptAndRestoreApprovedStatusWhenNoQuantityRemains() {
        OrdenCompra ordenCompra = ordenCompra();
        ordenCompra.getDetalles().get(0).setCantidadRecibida(2);
        ordenCompra.setEstado(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA);
        RecepcionAlmacen recepcion = recepcion(ordenCompra);
        when(recepcionRepository.findById(900L)).thenReturn(Optional.of(recepcion));
        when(currentUserService.getUsername()).thenReturn("admin");
        when(recepcionRepository.save(any(RecepcionAlmacen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.revertir(900L, new RecepcionReversionRequest("error de conteo"));

        assertEquals(EstadoRecepcionAlmacen.REVERTIDA, response.estado());
        assertEquals("admin", response.reversedBy());
        assertEquals("error de conteo", response.reversalReason());
        assertEquals(0, ordenCompra.getDetalles().get(0).getCantidadRecibida());
        assertEquals(EstadoOrdenCompra.APROBADA, ordenCompra.getEstado());
    }

    private RecepcionAlmacen recepcion(OrdenCompra ordenCompra) {
        RecepcionAlmacen recepcion = new RecepcionAlmacen();
        recepcion.setId(900L);
        recepcion.setNumero("REC-000900");
        recepcion.setOrdenCompra(ordenCompra);
        recepcion.setEstado(EstadoRecepcionAlmacen.REGISTRADA);
        recepcion.setReceivedAt(LocalDateTime.now());
        recepcion.setActor("compras");

        var detalle = new com.logistica.demo.logistica.compras.domain.RecepcionAlmacenDetalle();
        detalle.setId(910L);
        detalle.setOrdenCompraDetalle(ordenCompra.getDetalles().get(0));
        detalle.setCantidadRecibida(2);
        recepcion.addDetalle(detalle);
        return recepcion;
    }

    private OrdenCompra ordenCompra() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(20L);
        proveedor.setCode("PRV-020");
        proveedor.setName("Proveedor 20");

        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setId(600L);
        ordenCompra.setNumero("OC-000600");
        ordenCompra.setProveedor(proveedor);
        ordenCompra.setEstado(EstadoOrdenCompra.APROBADA);
        ordenCompra.setMoneda(Moneda.PEN);
        ordenCompra.setTipoCambio(BigDecimal.ONE);
        ordenCompra.setGeneratedAt(LocalDateTime.now());
        ordenCompra.setApprovedAt(LocalDateTime.now());
        ordenCompra.setApprovedBy("aprobador");
        ordenCompra.setSubtotal(new BigDecimal("30.00"));
        ordenCompra.setIgv(new BigDecimal("5.40"));
        ordenCompra.setTotal(new BigDecimal("35.40"));

        OrdenCompraDetalle detalle = new OrdenCompraDetalle();
        detalle.setId(610L);
        detalle.setItem(item());
        detalle.setAlmacen(almacen());
        detalle.setCantidad(3);
        detalle.setCantidadRecibida(0);
        detalle.setPrecioUnitario(new BigDecimal("10.00"));
        detalle.setSubtotalLinea(new BigDecimal("30.00"));
        ordenCompra.addDetalle(detalle);
        return ordenCompra;
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
