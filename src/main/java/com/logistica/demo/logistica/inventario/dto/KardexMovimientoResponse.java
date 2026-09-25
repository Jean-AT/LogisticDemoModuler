package com.logistica.demo.logistica.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KardexMovimientoResponse(
        Long id,
        Long itemId,
        String itemCode,
        String itemName,
        Long almacenId,
        String almacenCode,
        String almacenName,
        String tipo,
        String sourceType,
        Long sourceId,
        Long sourceLineId,
        String sourceNumber,
        BigDecimal cantidadDelta,
        BigDecimal saldo,
        String actor,
        LocalDateTime occurredAt) {
}
