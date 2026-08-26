package com.logistica.demo.requerimientos.dto;

import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import java.time.LocalDateTime;

public record RequerimientoEstadoHistorialResponse(
        Long id,
        EstadoRequerimiento estadoAnterior,
        EstadoRequerimiento estadoNuevo,
        String comentario,
        String usuario,
        LocalDateTime fechaHora) {
}
