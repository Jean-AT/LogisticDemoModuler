package com.logistica.demo.logistica.compras.dto;

import com.logistica.demo.logistica.compras.domain.EstadoRecepcionAlmacen;
import java.time.LocalDateTime;
import java.util.List;

public record RecepcionAlmacenResponse(
        Long id,
        String numero,
        Long ordenCompraId,
        String ordenCompraNumero,
        EstadoRecepcionAlmacen estado,
        LocalDateTime receivedAt,
        String actor,
        LocalDateTime reversedAt,
        String reversedBy,
        String reversalReason,
        List<RecepcionAlmacenDetalleResponse> detalles,
        String createdBy,
        LocalDateTime createdAt) {
}
