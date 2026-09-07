package com.logistica.demo.requerimientos.dto;

import com.logistica.demo.sharedkernel.domain.Moneda;
import java.util.List;

public record RequerimientoCreateRequest(
        String descripcion,
        Long proveedorId,
        Moneda moneda,
        List<RequerimientoDetalleRequest> detalles) {
}
