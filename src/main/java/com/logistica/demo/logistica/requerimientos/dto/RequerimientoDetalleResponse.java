package com.logistica.demo.logistica.requerimientos.dto;

import java.math.BigDecimal;

public record RequerimientoDetalleResponse(
        Long id,
        Long itemId,
        String itemCode,
        String itemName,
        Long almacenId,
        String almacenCode,
        String almacenName,
        Integer cantidad,
        BigDecimal precioUnitarioEstimado,
        BigDecimal subtotalLinea) {
}
