package com.logistica.demo.logistica.compras.dto;

public record RecepcionLineaRequest(
        Long ordenCompraDetalleId,
        Integer cantidadRecibida) {
}
