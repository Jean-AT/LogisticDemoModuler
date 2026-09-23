package com.logistica.demo.logistica.compras.dto;

import com.logistica.demo.logistica.compras.domain.EstadoProcesoCotizacion;
import java.time.LocalDateTime;
import java.util.List;

public record ProcesoCotizacionResponse(
        Long id,
        Long requerimientoId,
        String requerimientoNumero,
        EstadoProcesoCotizacion estado,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        List<CotizacionProveedorResponse> cotizaciones,
        AdjudicacionResponse adjudicacion) {
}
