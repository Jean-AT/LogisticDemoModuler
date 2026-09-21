package com.logistica.demo.logistica.requerimientos.dto;

import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;

public record OrdenCompraResumenResponse(
        Long id,
        String numero,
        Moneda moneda,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total) {
}
