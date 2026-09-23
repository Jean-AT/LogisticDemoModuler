package com.logistica.demo.logistica.compras.dto;

import java.util.List;

public record RecepcionAlmacenRequest(List<RecepcionLineaRequest> lineas) {

    public RecepcionAlmacenRequest {
        lineas = lineas == null ? List.of() : List.copyOf(lineas);
    }
}
