package com.logistica.demo.compras.dto;

import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.shared.domain.Moneda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenCompraResponse(
        Long id,
        String numero,
        Long requerimientoId,
        String requerimientoNumero,
        ProveedorResponse proveedor,
        Moneda moneda,
        BigDecimal tipoCambio,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        LocalDateTime generatedAt,
        List<OrdenCompraDetalleResponse> detalles,
        String createdBy,
        LocalDateTime createdAt) {
}
