package com.logistica.demo.logistica.compras.dto;

import com.logistica.demo.logistica.compras.domain.EstadoCotizacionProveedor;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CotizacionProveedorResponse(
        Long id,
        ProveedorResponse proveedor,
        Moneda moneda,
        EstadoCotizacionProveedor estado,
        LocalDateTime submittedAt,
        BigDecimal total,
        List<CotizacionLineaResponse> detalles) {
}
