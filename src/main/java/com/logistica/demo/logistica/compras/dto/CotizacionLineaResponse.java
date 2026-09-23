package com.logistica.demo.logistica.compras.dto;

import java.math.BigDecimal;

public record CotizacionLineaResponse(
        Long id,
        Long requerimientoDetalleId,
        Long itemId,
        String itemCode,
        String itemName,
        Integer cantidadOfertada,
        BigDecimal precioUnitario,
        BigDecimal subtotalLinea) {
}
