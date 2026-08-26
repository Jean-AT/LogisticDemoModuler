package com.logistica.demo.compras.dto;

import java.math.BigDecimal;

public record OrdenCompraDetalleResponse(
        Long id,
        Long itemId,
        String itemCode,
        String itemName,
        Long almacenId,
        String almacenCode,
        String almacenName,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotalLinea) {
}
