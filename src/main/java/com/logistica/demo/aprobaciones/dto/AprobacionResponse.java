package com.logistica.demo.aprobaciones.dto;

import com.logistica.demo.aprobaciones.domain.AccionAprobacion;
import java.time.LocalDateTime;

public record AprobacionResponse(
        Long id,
        AccionAprobacion accion,
        String comentario,
        LocalDateTime decisionAt,
        String usuario) {
}
