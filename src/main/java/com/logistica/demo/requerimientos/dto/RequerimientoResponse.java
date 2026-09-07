package com.logistica.demo.requerimientos.dto;

import com.logistica.demo.aprobaciones.dto.AprobacionResponse;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.time.LocalDateTime;
import java.util.List;

public record RequerimientoResponse(
        Long id,
        String numero,
        String descripcion,
        EstadoRequerimiento estado,
        Moneda moneda,
        ProveedorResponse proveedor,
        List<RequerimientoDetalleResponse> detalles,
        List<AprobacionResponse> aprobaciones,
        List<RequerimientoEstadoHistorialResponse> historialEstados,
        OrdenCompraResumenResponse ordenCompra,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt) {
}
