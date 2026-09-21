package com.logistica.demo.logistica.requerimientos.dto;

import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;

public record RequerimientoFromNeedsLineRequest(
        String descripcion,
        Long proveedorId,
        Moneda moneda,
        Long companyId,
        Long needsLineId,
        Long almacenId,
        Integer cantidad,
        BigDecimal precioUnitarioEstimado) {
}
