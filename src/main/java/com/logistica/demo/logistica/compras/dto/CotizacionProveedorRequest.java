package com.logistica.demo.logistica.compras.dto;

import com.logistica.demo.sharedkernel.domain.Moneda;
import java.util.List;

public record CotizacionProveedorRequest(
        Long proveedorId,
        Moneda moneda,
        List<CotizacionLineaRequest> lineas) {
}
