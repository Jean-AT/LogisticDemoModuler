package com.logistica.demo.logistica.inventario.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistica.demo.logistica.compras.domain.OrdenCompraDetalle;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacen;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacenDetalle;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class InventoryServiceTest {

    private JdbcTemplate jdbcTemplate;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new InventoryService(jdbcTemplate);
    }

    @Test
    void shouldRegisterReceiptMovement() {
        RecepcionAlmacen receipt = receipt();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("RECEPCION_ALMACEN"), eq(910L)))
                .thenReturn(0);

        service.registerReceipt(receipt);

        verify(jdbcTemplate).update(
                anyString(),
                eq(77L),
                eq(33L),
                eq("ENTRADA_RECEPCION"),
                eq("RECEPCION_ALMACEN"),
                eq(900L),
                eq(910L),
                eq("REC-000900"),
                eq(java.math.BigDecimal.valueOf(2)),
                eq("compras"),
                any(Timestamp.class));
    }

    @Test
    void shouldSkipExistingReceiptMovement() {
        RecepcionAlmacen receipt = receipt();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("RECEPCION_ALMACEN"), eq(910L)))
                .thenReturn(1);

        service.registerReceipt(receipt);

        verify(jdbcTemplate, times(0)).update(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void shouldRegisterReceiptReversalMovement() {
        RecepcionAlmacen receipt = receipt();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("REVERSION_RECEPCION"), eq(910L)))
                .thenReturn(0);

        service.registerReceiptReversal(receipt);

        verify(jdbcTemplate).update(
                anyString(),
                eq(77L),
                eq(33L),
                eq("REVERSA_RECEPCION"),
                eq("REVERSION_RECEPCION"),
                eq(900L),
                eq(910L),
                eq("REC-000900"),
                eq(java.math.BigDecimal.valueOf(2).negate()),
                eq("compras"),
                any(Timestamp.class));
    }

    private RecepcionAlmacen receipt() {
        Item item = new Item();
        item.setId(77L);
        item.setCode("SERV-001");
        item.setName("Servicio Demo");

        Almacen almacen = new Almacen();
        almacen.setId(33L);
        almacen.setCode("ALM-001");
        almacen.setName("Almacen Demo");

        OrdenCompraDetalle orderLine = new OrdenCompraDetalle();
        orderLine.setId(610L);
        orderLine.setItem(item);
        orderLine.setAlmacen(almacen);

        RecepcionAlmacenDetalle receiptLine = new RecepcionAlmacenDetalle();
        receiptLine.setId(910L);
        receiptLine.setOrdenCompraDetalle(orderLine);
        receiptLine.setCantidadRecibida(2);

        RecepcionAlmacen receipt = new RecepcionAlmacen();
        receipt.setId(900L);
        receipt.setNumero("REC-000900");
        receipt.setActor("compras");
        receipt.setReceivedAt(LocalDateTime.parse("2026-09-25T09:30:00"));
        receipt.addDetalle(receiptLine);
        return receipt;
    }
}
