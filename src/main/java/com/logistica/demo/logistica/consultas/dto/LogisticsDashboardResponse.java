package com.logistica.demo.logistica.consultas.dto;

import java.math.BigDecimal;

public record LogisticsDashboardResponse(
        long requerimientosPendientesAprobacion,
        long requerimientosAprobadosSinOc,
        long ordenesGeneradas,
        long ordenesAprobadas,
        long ordenesParcialmenteRecibidas,
        long ordenesRecibidas,
        long recepcionesRegistradasHoy,
        long recepcionesRevertidas,
        long posicionesStock,
        BigDecimal stockTotal,
        BigDecimal cantidadPendienteRecepcion,
        BigDecimal montoOrdenesAprobadas) {
}
