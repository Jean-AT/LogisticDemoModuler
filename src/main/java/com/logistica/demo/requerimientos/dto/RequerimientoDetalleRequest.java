package com.logistica.demo.requerimientos.dto;

import java.math.BigDecimal;

public record RequerimientoDetalleRequest(
        Long itemId,
        Long almacenId,
        Integer cantidad,
        BigDecimal precioUnitarioEstimado) {
}
