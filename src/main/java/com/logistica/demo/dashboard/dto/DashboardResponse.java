package com.logistica.demo.dashboard.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalRequerimientos,
        long borradores,
        long pendientesAprobacion,
        long observados,
        long rechazados,
        long aprobados,
        long convertidosOC,
        long pendientesPorGenerarOC,
        long enviadosHoy,
        long aprobadosHoy,
        long ocGeneradasHoy,
        BigDecimal montoEstimadoPendiente,
        MiActividad miActividad) {

    public record MiActividad(
            long total,
            long borradores,
            long enviados,
            long observados,
            long aprobados,
            long rechazados,
            BigDecimal montoEstimado) {
    }
}
