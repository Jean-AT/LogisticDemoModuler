package com.logistica.demo.logistica.compras.dto;

import java.math.BigDecimal;

public record CotizacionLineaRequest(
        Long requerimientoDetalleId,
        Integer cantidadOfertada,
        BigDecimal precioUnitario) {
}
