package com.logistica.demo.logistica.compras.dto;

import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.logistica.compras.domain.EstadoOrdenCompra;
import com.logistica.demo.sharedkernel.domain.Moneda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenCompraResponse(
        Long id,
        String numero,
        Long requerimientoId,
        String requerimientoNumero,
        Long adjudicacionId,
        ProveedorResponse proveedor,
        EstadoOrdenCompra estado,
        Long budgetControlId,
        Moneda moneda,
        BigDecimal tipoCambio,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        LocalDateTime generatedAt,
        LocalDateTime approvedAt,
        String approvedBy,
        List<OrdenCompraDetalleResponse> detalles,
        String createdBy,
        LocalDateTime createdAt) {
}
