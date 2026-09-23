package com.logistica.demo.logistica.compras.dto;

public record RecepcionAlmacenDetalleResponse(
        Long id,
        Long ordenCompraDetalleId,
        Long itemId,
        String itemCode,
        String itemName,
        Long almacenId,
        String almacenCode,
        String almacenName,
        Integer cantidadRecibida) {
}
