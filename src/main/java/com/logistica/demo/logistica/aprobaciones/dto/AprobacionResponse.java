package com.logistica.demo.logistica.aprobaciones.dto;

import com.logistica.demo.logistica.aprobaciones.domain.AccionAprobacion;
import java.time.LocalDateTime;

public record AprobacionResponse(
        Long id,
        AccionAprobacion accion,
        String comentario,
        LocalDateTime decisionAt,
        String usuario) {
}
