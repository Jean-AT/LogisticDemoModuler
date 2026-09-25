package com.logistica.demo.logistica.inventario.dto;

import java.math.BigDecimal;

public record StockProjectionResponse(
        Long itemId,
        String itemCode,
        String itemName,
        Long almacenId,
        String almacenCode,
        String almacenName,
        BigDecimal stockActual,
        BigDecimal cantidadPendienteOrdenes,
        BigDecimal stockProyectado) {
}
