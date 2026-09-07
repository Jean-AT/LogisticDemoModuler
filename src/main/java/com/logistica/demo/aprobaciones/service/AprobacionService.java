package com.logistica.demo.aprobaciones.service;

import com.logistica.demo.aprobaciones.domain.AccionAprobacion;
import com.logistica.demo.aprobaciones.domain.Aprobacion;
import com.logistica.demo.aprobaciones.dto.AprobacionDecisionRequest;
import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.domain.Requerimiento;
import com.logistica.demo.requerimientos.dto.RequerimientoResponse;
import com.logistica.demo.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.requerimientos.service.RequerimientoService;
import com.logistica.demo.sharedkernel.web.PageResponse;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobacionService {

    private final RequerimientoRepository requerimientoRepository;
    private final RequerimientoService requerimientoService;

    public AprobacionService(RequerimientoRepository requerimientoRepository, RequerimientoService requerimientoService) {
        this.requerimientoRepository = requerimientoRepository;
        this.requerimientoService = requerimientoService;
    }

    @Transactional
    public RequerimientoResponse aprobar(Long requerimientoId, AprobacionDecisionRequest request) {
        return aplicarDecision(requerimientoId, AccionAprobacion.APROBAR, request);
    }

    @Transactional
    public RequerimientoResponse observar(Long requerimientoId, AprobacionDecisionRequest request) {
        return aplicarDecision(requerimientoId, AccionAprobacion.OBSERVAR, request);
    }

    @Transactional
    public RequerimientoResponse rechazar(Long requerimientoId, AprobacionDecisionRequest request) {
        return aplicarDecision(requerimientoId, AccionAprobacion.RECHAZAR, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequerimientoResponse> list(
            Long id,
            EstadoRequerimiento estado,
            String numero,
            Long proveedorId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {
        return requerimientoService.list(id, estado, numero, proveedorId, fechaDesde, fechaHasta, page, size);
    }
    @Transactional(readOnly = true)
    public RequerimientoResponse getByRequerimientoId(Long requerimientoId) {
        return requerimientoService.getById(requerimientoId);
    }

    private RequerimientoResponse aplicarDecision(
            Long requerimientoId,
            AccionAprobacion accion,
            AprobacionDecisionRequest request) {
        Requerimiento requerimiento = requerimientoRepository.findById(requerimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento no encontrado."));

        if (requerimiento.getEstado() != EstadoRequerimiento.ENVIADO
                && requerimiento.getEstado() != EstadoRequerimiento.OBSERVADO) {
            throw new BusinessRuleException("Solo se puede decidir un requerimiento en estado ENVIADO u OBSERVADO.");
        }

        String comentario = request != null ? request.comentario() : null;
        if ((accion == AccionAprobacion.OBSERVAR || accion == AccionAprobacion.RECHAZAR)
                && (comentario == null || comentario.isBlank())) {
            throw new BusinessRuleException("El comentario es obligatorio para observar o rechazar.");
        }

        Aprobacion aprobacion = new Aprobacion();
        aprobacion.setAccion(accion);
        aprobacion.setComentario(comentario == null ? null : comentario.trim());
        aprobacion.setDecisionAt(LocalDateTime.now());
        requerimiento.addAprobacion(aprobacion);
        requerimiento.cambiarEstado(resolveEstado(accion), comentario);

        return requerimientoService.mapResponse(requerimientoRepository.saveAndFlush(requerimiento));
    }

    private EstadoRequerimiento resolveEstado(AccionAprobacion accion) {
        return switch (accion) {
            case APROBAR -> EstadoRequerimiento.APROBADO;
            case OBSERVAR -> EstadoRequerimiento.OBSERVADO;
            case RECHAZAR -> EstadoRequerimiento.RECHAZADO;
        };
    }
}
