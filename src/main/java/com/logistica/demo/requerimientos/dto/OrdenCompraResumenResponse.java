package com.logistica.demo.requerimientos.dto;

import com.logistica.demo.shared.domain.Moneda;
import java.math.BigDecimal;

public record OrdenCompraResumenResponse(
        Long id,
        String numero,
        Moneda moneda,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total) {
}
