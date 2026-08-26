package com.logistica.demo.dashboard.service;

import com.logistica.demo.compras.repository.OrdenCompraRepository;
import com.logistica.demo.dashboard.dto.DashboardResponse;
import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.shared.security.CurrentUserService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final RequerimientoRepository requerimientoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final CurrentUserService currentUserService;

    public DashboardService(
            RequerimientoRepository requerimientoRepository,
            OrdenCompraRepository ordenCompraRepository,
            CurrentUserService currentUserService) {
        this.requerimientoRepository = requerimientoRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        String owner = currentUserService.hasGlobalRequisitionAccess() ? null : currentUserService.getUsername();
        String username = currentUserService.getUsername();
        LocalDate today = LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        DashboardResponse.MiActividad miActividad = buildMiActividad(username);

        return new DashboardResponse(
                requerimientoRepository.countByOwner(owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.BORRADOR, owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.ENVIADO, owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.OBSERVADO, owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.RECHAZADO, owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.APROBADO, owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.CONVERTIDO_OC, owner),
                requerimientoRepository.countByEstadoAndOwner(EstadoRequerimiento.APROBADO, owner),
                requerimientoRepository.countByEstadoSince(EstadoRequerimiento.ENVIADO, startOfDay, owner),
                requerimientoRepository.countByEstadoSince(EstadoRequerimiento.APROBADO, startOfDay, owner),
                ordenCompraRepository.countGeneratedSince(startOfDay, startOfTomorrow, owner),
                requerimientoRepository.sumDetalleSubtotalByEstado(EstadoRequerimiento.ENVIADO, owner),
                miActividad);
    }

    private DashboardResponse.MiActividad buildMiActividad(String username) {
        long total = requerimientoRepository.countByCreatedBy(username);
        return new DashboardResponse.MiActividad(
                total,
                requerimientoRepository.countByEstadoAndCreatedBy(EstadoRequerimiento.BORRADOR, username),
                requerimientoRepository.countByEstadoAndCreatedBy(EstadoRequerimiento.ENVIADO, username),
                requerimientoRepository.countByEstadoAndCreatedBy(EstadoRequerimiento.OBSERVADO, username),
                requerimientoRepository.countByEstadoAndCreatedBy(EstadoRequerimiento.APROBADO, username),
                requerimientoRepository.countByEstadoAndCreatedBy(EstadoRequerimiento.RECHAZADO, username),
                requerimientoRepository.sumDetalleSubtotalByEstado(EstadoRequerimiento.ENVIADO, username));
    }
}
