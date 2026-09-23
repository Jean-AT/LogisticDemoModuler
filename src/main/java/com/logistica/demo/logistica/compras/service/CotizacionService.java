package com.logistica.demo.logistica.compras.service;

import com.logistica.demo.logistica.compras.domain.Adjudicacion;
import com.logistica.demo.logistica.compras.domain.CotizacionProveedor;
import com.logistica.demo.logistica.compras.domain.CotizacionProveedorDetalle;
import com.logistica.demo.logistica.compras.domain.EstadoCotizacionProveedor;
import com.logistica.demo.logistica.compras.domain.EstadoProcesoCotizacion;
import com.logistica.demo.logistica.compras.domain.ProcesoCotizacion;
import com.logistica.demo.logistica.compras.dto.AdjudicacionRequest;
import com.logistica.demo.logistica.compras.dto.AdjudicacionResponse;
import com.logistica.demo.logistica.compras.dto.CotizacionLineaRequest;
import com.logistica.demo.logistica.compras.dto.CotizacionLineaResponse;
import com.logistica.demo.logistica.compras.dto.CotizacionProveedorRequest;
import com.logistica.demo.logistica.compras.dto.CotizacionProveedorResponse;
import com.logistica.demo.logistica.compras.dto.ProcesoCotizacionResponse;
import com.logistica.demo.logistica.compras.repository.CotizacionProveedorRepository;
import com.logistica.demo.logistica.compras.repository.ProcesoCotizacionRepository;
import com.logistica.demo.logistica.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.logistica.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.maestros.repository.ProveedorRepository;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CotizacionService {

    private final ProcesoCotizacionRepository procesoRepository;
    private final CotizacionProveedorRepository cotizacionRepository;
    private final RequerimientoRepository requerimientoRepository;
    private final ProveedorRepository proveedorRepository;
    private final CurrentUserService currentUserService;

    public CotizacionService(
            ProcesoCotizacionRepository procesoRepository,
            CotizacionProveedorRepository cotizacionRepository,
            RequerimientoRepository requerimientoRepository,
            ProveedorRepository proveedorRepository,
            CurrentUserService currentUserService) {
        this.procesoRepository = procesoRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.requerimientoRepository = requerimientoRepository;
        this.proveedorRepository = proveedorRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ProcesoCotizacionResponse abrirDesdeRequerimiento(Long requerimientoId) {
        Requerimiento requerimiento = requerimientoRepository.findById(requerimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento no encontrado."));
        if (requerimiento.getEstado() != EstadoRequerimiento.APROBADO) {
            throw new BusinessRuleException("Solo se puede cotizar un requerimiento APROBADO.");
        }
        if (requerimiento.getNeedsLineId() != null && requerimiento.getBudgetControlId() == null) {
            throw new BusinessRuleException("Sin precompromiso presupuestal no se puede iniciar compra.");
        }
        if (procesoRepository.existsByRequerimientoId(requerimientoId)) {
            throw new BusinessRuleException("Ya existe un proceso de cotizacion para este requerimiento.");
        }

        ProcesoCotizacion proceso = new ProcesoCotizacion();
        proceso.setRequerimiento(requerimiento);
        proceso.setEstado(EstadoProcesoCotizacion.ABIERTO);
        proceso.setOpenedAt(LocalDateTime.now());
        return mapResponse(procesoRepository.saveAndFlush(proceso));
    }

    @Transactional
    public ProcesoCotizacionResponse registrarCotizacion(Long procesoId, CotizacionProveedorRequest request) {
        ProcesoCotizacion proceso = findProceso(procesoId);
        if (proceso.getEstado() != EstadoProcesoCotizacion.ABIERTO) {
            throw new BusinessRuleException("Solo se pueden registrar cotizaciones en un proceso ABIERTO.");
        }
        validateQuoteRequest(request);
        if (cotizacionRepository.findByProcesoIdAndProveedorId(procesoId, request.proveedorId()).isPresent()) {
            throw new BusinessRuleException("El proveedor ya presento una cotizacion para este proceso.");
        }
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .filter(Proveedor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));

        CotizacionProveedor cotizacion = new CotizacionProveedor();
        cotizacion.setProveedor(proveedor);
        cotizacion.setMoneda(request.moneda());
        cotizacion.setEstado(EstadoCotizacionProveedor.PRESENTADA);
        cotizacion.setSubmittedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (CotizacionProveedorDetalle detalle : buildDetalles(proceso.getRequerimiento(), request.lineas())) {
            total = total.add(detalle.getSubtotalLinea());
            cotizacion.addDetalle(detalle);
        }
        cotizacion.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        proceso.addCotizacion(cotizacion);
        return mapResponse(procesoRepository.saveAndFlush(proceso));
    }

    @Transactional
    public ProcesoCotizacionResponse cerrar(Long procesoId) {
        ProcesoCotizacion proceso = findProceso(procesoId);
        if (proceso.getEstado() != EstadoProcesoCotizacion.ABIERTO) {
            throw new BusinessRuleException("Solo se puede cerrar un proceso ABIERTO.");
        }
        boolean hasValidQuote = proceso.getCotizaciones().stream()
                .anyMatch(cotizacion -> cotizacion.getEstado() == EstadoCotizacionProveedor.PRESENTADA);
        if (!hasValidQuote) {
            throw new BusinessRuleException("Debe existir al menos una cotizacion valida para cerrar el proceso.");
        }
        proceso.setEstado(EstadoProcesoCotizacion.CERRADO);
        proceso.setClosedAt(LocalDateTime.now());
        return mapResponse(procesoRepository.saveAndFlush(proceso));
    }

    @Transactional
    public ProcesoCotizacionResponse adjudicar(Long procesoId, AdjudicacionRequest request) {
        ProcesoCotizacion proceso = findProceso(procesoId);
        if (proceso.getEstado() != EstadoProcesoCotizacion.CERRADO) {
            throw new BusinessRuleException("Solo se puede adjudicar un proceso CERRADO.");
        }
        if (request == null || request.cotizacionId() == null || request.cotizacionId() <= 0) {
            throw new BadRequestException("La cotizacion es obligatoria.");
        }
        CotizacionProveedor cotizacion = proceso.getCotizaciones().stream()
                .filter(candidate -> candidate.getId().equals(request.cotizacionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cotizacion no encontrada para este proceso."));
        if (cotizacion.getEstado() != EstadoCotizacionProveedor.PRESENTADA) {
            throw new BusinessRuleException("Solo se puede adjudicar una cotizacion presentada.");
        }

        cotizacion.setEstado(EstadoCotizacionProveedor.ADJUDICADA);
        Adjudicacion adjudicacion = new Adjudicacion();
        adjudicacion.setCotizacion(cotizacion);
        adjudicacion.setAwardedAt(LocalDateTime.now());
        adjudicacion.setActor(currentUserService.getUsername());
        proceso.setAdjudicacion(adjudicacion);
        proceso.setEstado(EstadoProcesoCotizacion.ADJUDICADO);
        return mapResponse(procesoRepository.saveAndFlush(proceso));
    }

    @Transactional(readOnly = true)
    public ProcesoCotizacionResponse getById(Long procesoId) {
        return mapResponse(findProceso(procesoId));
    }

    private ProcesoCotizacion findProceso(Long procesoId) {
        return procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proceso de cotizacion no encontrado."));
    }

    private void validateQuoteRequest(CotizacionProveedorRequest request) {
        if (request == null) {
            throw new BadRequestException("La cotizacion es obligatoria.");
        }
        if (request.proveedorId() == null || request.proveedorId() <= 0) {
            throw new BadRequestException("El proveedor es obligatorio.");
        }
        if (request.moneda() == null) {
            throw new BadRequestException("La moneda es obligatoria.");
        }
        if (request.lineas() == null || request.lineas().isEmpty()) {
            throw new BadRequestException("La cotizacion debe tener lineas.");
        }
    }

    private List<CotizacionProveedorDetalle> buildDetalles(
            Requerimiento requerimiento,
            List<CotizacionLineaRequest> lineRequests) {
        Map<Long, RequerimientoDetalle> detallesRequerimiento = requerimiento.getDetalles().stream()
                .collect(Collectors.toMap(RequerimientoDetalle::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        List<CotizacionProveedorDetalle> detalles = lineRequests.stream()
                .map(line -> buildDetalle(line, detallesRequerimiento, seen))
                .toList();
        if (!seen.equals(detallesRequerimiento.keySet())) {
            throw new BusinessRuleException("La cotizacion debe cubrir todas las lineas del requerimiento.");
        }
        return detalles;
    }

    private CotizacionProveedorDetalle buildDetalle(
            CotizacionLineaRequest request,
            Map<Long, RequerimientoDetalle> detallesRequerimiento,
            Set<Long> seen) {
        if (request.requerimientoDetalleId() == null || request.requerimientoDetalleId() <= 0) {
            throw new BadRequestException("La linea del requerimiento es obligatoria.");
        }
        if (!seen.add(request.requerimientoDetalleId())) {
            throw new BusinessRuleException("La cotizacion contiene lineas duplicadas.");
        }
        RequerimientoDetalle requerimientoDetalle = detallesRequerimiento.get(request.requerimientoDetalleId());
        if (requerimientoDetalle == null) {
            throw new ResourceNotFoundException("Linea del requerimiento no encontrada.");
        }
        if (request.cantidadOfertada() == null || request.cantidadOfertada() <= 0) {
            throw new BadRequestException("La cantidad ofertada debe ser mayor que cero.");
        }
        if (!request.cantidadOfertada().equals(requerimientoDetalle.getCantidad())) {
            throw new BusinessRuleException("La cantidad ofertada debe cubrir la cantidad requerida.");
        }
        if (request.precioUnitario() == null || request.precioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El precio unitario no puede ser negativo.");
        }

        CotizacionProveedorDetalle detalle = new CotizacionProveedorDetalle();
        detalle.setRequerimientoDetalle(requerimientoDetalle);
        detalle.setCantidadOfertada(request.cantidadOfertada());
        detalle.setPrecioUnitario(request.precioUnitario());
        detalle.setSubtotalLinea(request.precioUnitario()
                .multiply(BigDecimal.valueOf(request.cantidadOfertada().longValue()))
                .setScale(2, RoundingMode.HALF_UP));
        return detalle;
    }

    public ProcesoCotizacionResponse mapResponse(ProcesoCotizacion proceso) {
        return new ProcesoCotizacionResponse(
                proceso.getId(),
                proceso.getRequerimiento().getId(),
                proceso.getRequerimiento().getNumero(),
                proceso.getEstado(),
                proceso.getOpenedAt(),
                proceso.getClosedAt(),
                proceso.getCotizaciones().stream().map(this::mapCotizacion).toList(),
                mapAdjudicacion(proceso.getAdjudicacion()));
    }

    private CotizacionProveedorResponse mapCotizacion(CotizacionProveedor cotizacion) {
        Proveedor proveedor = cotizacion.getProveedor();
        return new CotizacionProveedorResponse(
                cotizacion.getId(),
                new ProveedorResponse(proveedor.getId(), proveedor.getCode(), proveedor.getName()),
                cotizacion.getMoneda(),
                cotizacion.getEstado(),
                cotizacion.getSubmittedAt(),
                cotizacion.getTotal(),
                cotizacion.getDetalles().stream().map(this::mapDetalle).toList());
    }

    private CotizacionLineaResponse mapDetalle(CotizacionProveedorDetalle detalle) {
        RequerimientoDetalle requerimientoDetalle = detalle.getRequerimientoDetalle();
        return new CotizacionLineaResponse(
                detalle.getId(),
                requerimientoDetalle.getId(),
                requerimientoDetalle.getItem().getId(),
                requerimientoDetalle.getItem().getCode(),
                requerimientoDetalle.getItem().getName(),
                detalle.getCantidadOfertada(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotalLinea());
    }

    private AdjudicacionResponse mapAdjudicacion(Adjudicacion adjudicacion) {
        if (adjudicacion == null) {
            return null;
        }
        Proveedor proveedor = adjudicacion.getCotizacion().getProveedor();
        return new AdjudicacionResponse(
                adjudicacion.getId(),
                adjudicacion.getCotizacion().getId(),
                proveedor.getId(),
                proveedor.getName(),
                adjudicacion.getAwardedAt(),
                adjudicacion.getActor());
    }
}
