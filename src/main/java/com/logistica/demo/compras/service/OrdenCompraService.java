package com.logistica.demo.compras.service;

import com.logistica.demo.compras.domain.OrdenCompra;
import com.logistica.demo.compras.domain.OrdenCompraDetalle;
import com.logistica.demo.compras.dto.OrdenCompraDetalleResponse;
import com.logistica.demo.compras.dto.OrdenCompraResponse;
import com.logistica.demo.compras.repository.OrdenCompraRepository;
import com.logistica.demo.finanzas.service.DocumentTotals;
import com.logistica.demo.finanzas.service.FinancialCalculatorService;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.domain.Requerimiento;
import com.logistica.demo.requerimientos.domain.RequerimientoDetalle;
import com.logistica.demo.requerimientos.repository.RequerimientoRepository;
import com.logistica.demo.sharedkernel.domain.Moneda;
import com.logistica.demo.sharedkernel.web.PageResponse;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final RequerimientoRepository requerimientoRepository;
    private final FinancialCalculatorService financialCalculatorService;

    public OrdenCompraService(
            OrdenCompraRepository ordenCompraRepository,
            RequerimientoRepository requerimientoRepository,
            FinancialCalculatorService financialCalculatorService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.requerimientoRepository = requerimientoRepository;
        this.financialCalculatorService = financialCalculatorService;
    }

    @Transactional
    public OrdenCompraResponse generarDesdeRequerimiento(Long requerimientoId) {
        Requerimiento requerimiento = requerimientoRepository.findById(requerimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento no encontrado."));

        if (requerimiento.getEstado() != EstadoRequerimiento.APROBADO) {
            throw new BusinessRuleException("Solo se puede generar una orden desde un requerimiento APROBADO.");
        }
        if (ordenCompraRepository.findByRequerimientoId(requerimientoId).isPresent()) {
            throw new BusinessRuleException("Ya existe una orden de compra para este requerimiento.");
        }

        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setRequerimiento(requerimiento);
        ordenCompra.setProveedor(requerimiento.getProveedor());
        ordenCompra.setMoneda(requerimiento.getMoneda());
        ordenCompra.setTipoCambio(financialCalculatorService.resolveExchangeRate(requerimiento.getMoneda()));
        ordenCompra.setGeneratedAt(LocalDateTime.now());

        List<OrdenCompraDetalle> detalles = new ArrayList<>();
        for (RequerimientoDetalle detalleReq : requerimiento.getDetalles()) {
            OrdenCompraDetalle detalle = new OrdenCompraDetalle();
            detalle.setItem(detalleReq.getItem());
            detalle.setAlmacen(detalleReq.getAlmacen());
            detalle.setCantidad(detalleReq.getCantidad());
            detalle.setPrecioUnitario(detalleReq.getPrecioUnitarioEstimado());
            detalle.setSubtotalLinea(detalleReq.getSubtotalLinea());
            detalles.add(detalle);
        }
        detalles.forEach(ordenCompra::addDetalle);

        DocumentTotals totals = financialCalculatorService.calculateDocumentTotals(
                detalles.stream().map(OrdenCompraDetalle::getSubtotalLinea).toList(),
                requerimiento.getMoneda());
        ordenCompra.setSubtotal(totals.subtotal());
        ordenCompra.setIgv(totals.igv());
        ordenCompra.setTotal(totals.total());

        OrdenCompra saved = ordenCompraRepository.save(ordenCompra);
        saved.setNumero("OC-%06d".formatted(saved.getId()));

        requerimiento.cambiarEstado(
                EstadoRequerimiento.CONVERTIDO_OC,
                "Orden de compra %s generada.".formatted(saved.getNumero()));
        requerimiento.setOrdenCompra(saved);

        return mapResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrdenCompraResponse> listar(
            String numero,
            Long proveedorId,
            Moneda moneda,
            Long requerimientoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BadRequestException("fechaDesde no puede ser mayor que fechaHasta.");
        }
        LocalDateTime inicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fin = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;
        String numeroFiltro = numero == null || numero.isBlank() ? null : numero.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "generatedAt"));
        Page<OrdenCompraResponse> result = ordenCompraRepository
                .findAll(buildSearchSpecification(numeroFiltro, proveedorId, moneda, requerimientoId, inicio, fin), pageRequest)
                .map(this::mapResponse);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse getById(Long id) {
        return ordenCompraRepository.findById(id)
                .map(this::mapResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada."));
    }

    public OrdenCompraResponse mapResponse(OrdenCompra ordenCompra) {
        return new OrdenCompraResponse(
                ordenCompra.getId(),
                ordenCompra.getNumero(),
                ordenCompra.getRequerimiento().getId(),
                ordenCompra.getRequerimiento().getNumero(),
                new ProveedorResponse(
                        ordenCompra.getProveedor().getId(),
                        ordenCompra.getProveedor().getCode(),
                        ordenCompra.getProveedor().getName()),
                ordenCompra.getMoneda(),
                ordenCompra.getTipoCambio(),
                ordenCompra.getSubtotal(),
                ordenCompra.getIgv(),
                ordenCompra.getTotal(),
                ordenCompra.getGeneratedAt(),
                ordenCompra.getDetalles().stream()
                        .map(detalle -> new OrdenCompraDetalleResponse(
                                detalle.getId(),
                                detalle.getItem().getId(),
                                detalle.getItem().getCode(),
                                detalle.getItem().getName(),
                                detalle.getAlmacen().getId(),
                                detalle.getAlmacen().getCode(),
                                detalle.getAlmacen().getName(),
                                detalle.getCantidad(),
                                detalle.getPrecioUnitario(),
                                detalle.getSubtotalLinea()))
                        .toList(),
                ordenCompra.getCreatedBy(),
                ordenCompra.getCreatedAt());
    }

    private Specification<OrdenCompra> buildSearchSpecification(
            String numero,
            Long proveedorId,
            Moneda moneda,
            Long requerimientoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (numero != null) {
                predicates.add(cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%"));
            }
            if (proveedorId != null) {
                predicates.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }
            if (moneda != null) {
                predicates.add(cb.equal(root.get("moneda"), moneda));
            }
            if (requerimientoId != null) {
                predicates.add(cb.equal(root.get("requerimiento").get("id"), requerimientoId));
            }
            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("generatedAt"), fechaInicio));
            }
            if (fechaFin != null) {
                predicates.add(cb.lessThan(root.get("generatedAt"), fechaFin));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
