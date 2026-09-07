package com.logistica.demo.requerimientos.service;

import com.logistica.demo.aprobaciones.dto.AprobacionResponse;
import com.logistica.demo.compras.domain.OrdenCompra;
import com.logistica.demo.finanzas.service.FinancialCalculatorService;
import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.maestros.repository.AlmacenRepository;
import com.logistica.demo.maestros.repository.ItemRepository;
import com.logistica.demo.maestros.repository.ProveedorRepository;
import com.logistica.demo.requerimientos.dto.RequerimientoEstadoHistorialResponse;
import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.domain.Requerimiento;
import com.logistica.demo.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.requerimientos.dto.OrdenCompraResumenResponse;
import com.logistica.demo.requerimientos.dto.RequerimientoCreateRequest;
import com.logistica.demo.requerimientos.dto.RequerimientoDetalleRequest;
import com.logistica.demo.requerimientos.dto.RequerimientoDetalleResponse;
import com.logistica.demo.requerimientos.dto.RequerimientoResponse;
import com.logistica.demo.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.sharedkernel.web.PageResponse;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.shared.security.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequerimientoService {

    private final RequerimientoRepository requerimientoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ItemRepository itemRepository;
    private final AlmacenRepository almacenRepository;
    private final FinancialCalculatorService financialCalculatorService;
    private final CurrentUserService currentUserService;

    public RequerimientoService(
            RequerimientoRepository requerimientoRepository,
            ProveedorRepository proveedorRepository,
            ItemRepository itemRepository,
            AlmacenRepository almacenRepository,
            FinancialCalculatorService financialCalculatorService,
            CurrentUserService currentUserService) {
        this.requerimientoRepository = requerimientoRepository;
        this.proveedorRepository = proveedorRepository;
        this.itemRepository = itemRepository;
        this.almacenRepository = almacenRepository;
        this.financialCalculatorService = financialCalculatorService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public RequerimientoResponse create(RequerimientoCreateRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .filter(Proveedor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));

        Requerimiento requerimiento = new Requerimiento();
        requerimiento.setDescripcion(request.descripcion().trim());
        requerimiento.setProveedor(proveedor);
        requerimiento.setMoneda(request.moneda());
        requerimiento.cambiarEstado(EstadoRequerimiento.BORRADOR, "Requerimiento creado.");
        requerimiento.replaceDetalles(buildDetalles(request.detalles()));

        Requerimiento saved = requerimientoRepository.saveAndFlush(requerimiento);
        saved.setNumero("REQ-%06d".formatted(saved.getId()));

        return mapResponse(saved);
    }

    @Transactional
    public RequerimientoResponse update(Long id, RequerimientoCreateRequest request) {
        Requerimiento requerimiento = findById(id);
        assertCanManage(requerimiento);
        if (requerimiento.getEstado() != EstadoRequerimiento.BORRADOR
                && requerimiento.getEstado() != EstadoRequerimiento.OBSERVADO) {
            throw new BusinessRuleException(
                    "Solo se puede editar un requerimiento en estado BORRADOR u OBSERVADO.");
        }
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessRuleException("El requerimiento debe tener al menos un detalle.");
        }

        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .filter(Proveedor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));

        requerimiento.setDescripcion(request.descripcion().trim());
        requerimiento.setProveedor(proveedor);
        requerimiento.setMoneda(request.moneda());
        requerimiento.replaceDetalles(buildDetalles(request.detalles()));

        if (requerimiento.getEstado() == EstadoRequerimiento.OBSERVADO) {
            requerimiento.cambiarEstado(
                    EstadoRequerimiento.BORRADOR,
                    "Requerimiento observado corregido por el solicitante.");
        }

        return mapResponse(requerimientoRepository.saveAndFlush(requerimiento));
    }

    @Transactional(readOnly = true)
    public PageResponse<RequerimientoResponse> list(
            EstadoRequerimiento estado,
            String numero,
            Long proveedorId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {
        return list(null, estado, numero, proveedorId, fechaDesde, fechaHasta, page, size);
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
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BadRequestException("fechaDesde no puede ser mayor que fechaHasta.");
        }
        LocalDateTime inicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fin = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;
        String ownerUsername = currentUserService.hasGlobalRequisitionAccess() ? null : currentUserService.getUsername();
        String numeroFiltro = numero == null || numero.isBlank() ? null : numero.trim();
        PageRequest pageRequest = PageRequest.of(normalizePage(page), normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequerimientoResponse> result = requerimientoRepository
                .findAll(buildSearchSpecification(id, estado, numeroFiltro, ownerUsername, proveedorId, inicio, fin), pageRequest)
                .map(this::mapResponse);
        return PageResponse.of(result);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    @Transactional(readOnly = true)
    public RequerimientoResponse getById(Long id) {
        Requerimiento requerimiento = findById(id);
        assertCanAccess(requerimiento);
        return mapResponse(requerimiento);
    }

    @Transactional
    public RequerimientoResponse enviar(Long id) {
        Requerimiento requerimiento = findById(id);
        assertCanManage(requerimiento);
        if (requerimiento.getEstado() != EstadoRequerimiento.BORRADOR) {
            throw new BusinessRuleException("Solo se puede enviar un requerimiento en estado BORRADOR.");
        }
        if (requerimiento.getDetalles().isEmpty()) {
            throw new BusinessRuleException("El requerimiento debe tener al menos un detalle.");
        }
        requerimiento.cambiarEstado(EstadoRequerimiento.ENVIADO, "Requerimiento enviado a aprobacion.");
        return mapResponse(requerimientoRepository.saveAndFlush(requerimiento));
    }

    private Requerimiento findById(Long id) {
        return requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento no encontrado."));
    }

    private List<RequerimientoDetalle> buildDetalles(List<RequerimientoDetalleRequest> requests) {
        List<RequerimientoDetalle> detalles = new ArrayList<>();
        for (RequerimientoDetalleRequest request : requests) {
            Item item = itemRepository.findById(request.itemId())
                    .filter(Item::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado."));
            Almacen almacen = almacenRepository.findById(request.almacenId())
                    .filter(Almacen::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Almacen no encontrado."));

            RequerimientoDetalle detalle = new RequerimientoDetalle();
            detalle.setItem(item);
            detalle.setAlmacen(almacen);
            detalle.setCantidad(request.cantidad());
            detalle.setPrecioUnitarioEstimado(request.precioUnitarioEstimado());
            detalle.setSubtotalLinea(financialCalculatorService.calculateLineSubtotal(
                    request.cantidad(),
                    request.precioUnitarioEstimado()));
            detalles.add(detalle);
        }
        return detalles;
    }

    public RequerimientoResponse mapResponse(Requerimiento requerimiento) {
        return new RequerimientoResponse(
                requerimiento.getId(),
                requerimiento.getNumero(),
                requerimiento.getDescripcion(),
                requerimiento.getEstado(),
                requerimiento.getMoneda(),
                new ProveedorResponse(
                        requerimiento.getProveedor().getId(),
                        requerimiento.getProveedor().getCode(),
                        requerimiento.getProveedor().getName()),
                requerimiento.getDetalles().stream()
                        .map(detalle -> new RequerimientoDetalleResponse(
                                detalle.getId(),
                                detalle.getItem().getId(),
                                detalle.getItem().getCode(),
                                detalle.getItem().getName(),
                                detalle.getAlmacen().getId(),
                                detalle.getAlmacen().getCode(),
                                detalle.getAlmacen().getName(),
                                detalle.getCantidad(),
                                detalle.getPrecioUnitarioEstimado(),
                                detalle.getSubtotalLinea()))
                        .toList(),
                requerimiento.getAprobaciones().stream()
                        .map(aprobacion -> new AprobacionResponse(
                                aprobacion.getId(),
                                aprobacion.getAccion(),
                                aprobacion.getComentario(),
                                aprobacion.getDecisionAt(),
                                aprobacion.getCreatedBy()))
                        .toList(),
                requerimiento.getHistorialEstados().stream()
                        .map(historial -> new RequerimientoEstadoHistorialResponse(
                                historial.getId(),
                                historial.getEstadoAnterior(),
                                historial.getEstadoNuevo(),
                                historial.getComentario(),
                                historial.getCreatedBy(),
                                historial.getCreatedAt()))
                        .toList(),
                mapOrdenCompra(requerimiento.getOrdenCompra()),
                requerimiento.getCreatedBy(),
                requerimiento.getCreatedAt(),
                requerimiento.getUpdatedBy(),
                requerimiento.getUpdatedAt());
    }

    private OrdenCompraResumenResponse mapOrdenCompra(OrdenCompra ordenCompra) {
        if (ordenCompra == null) {
            return null;
        }
        return new OrdenCompraResumenResponse(
                ordenCompra.getId(),
                ordenCompra.getNumero(),
                ordenCompra.getMoneda(),
                ordenCompra.getSubtotal(),
                ordenCompra.getIgv(),
                ordenCompra.getTotal());
    }

    private void assertCanAccess(Requerimiento requerimiento) {
        if (currentUserService.hasGlobalRequisitionAccess()
                || requerimiento.getCreatedBy().equalsIgnoreCase(currentUserService.getUsername())) {
            return;
        }
        throw new AccessDeniedException("No tiene permisos para acceder a este requerimiento.");
    }

    private void assertCanManage(Requerimiento requerimiento) {
        if (currentUserService.hasAnyRole(UserRole.ADMIN)
                || requerimiento.getCreatedBy().equalsIgnoreCase(currentUserService.getUsername())) {
            return;
        }
        throw new AccessDeniedException("No tiene permisos para modificar este requerimiento.");
    }

    private Specification<Requerimiento> buildSearchSpecification(
            Long id,
            EstadoRequerimiento estado,
            String numero,
            String createdBy,
            Long proveedorId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (id != null) {
                predicates.add(cb.equal(root.get("id"), id));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (numero != null) {
                predicates.add(cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%"));
            }
            if (createdBy != null) {
                predicates.add(cb.equal(cb.lower(root.get("createdBy")), createdBy.toLowerCase()));
            }
            if (proveedorId != null) {
                predicates.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }
            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fechaInicio));
            }
            if (fechaFin != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), fechaFin));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
