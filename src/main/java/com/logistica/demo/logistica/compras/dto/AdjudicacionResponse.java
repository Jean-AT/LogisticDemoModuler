package com.logistica.demo.logistica.compras.dto;

import java.time.LocalDateTime;

public record AdjudicacionResponse(
        Long id,
        Long cotizacionId,
        Long proveedorId,
        String proveedorName,
        LocalDateTime awardedAt,
        String actor) {
}
