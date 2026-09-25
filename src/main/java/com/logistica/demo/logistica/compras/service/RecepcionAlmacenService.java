package com.logistica.demo.logistica.compras.service;

import com.logistica.demo.logistica.compras.domain.EstadoOrdenCompra;
import com.logistica.demo.logistica.compras.domain.EstadoRecepcionAlmacen;
import com.logistica.demo.logistica.compras.domain.OrdenCompra;
import com.logistica.demo.logistica.compras.domain.OrdenCompraDetalle;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacen;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacenDetalle;
import com.logistica.demo.logistica.compras.dto.RecepcionAlmacenDetalleResponse;
import com.logistica.demo.logistica.compras.dto.RecepcionAlmacenRequest;
import com.logistica.demo.logistica.compras.dto.RecepcionAlmacenResponse;
import com.logistica.demo.logistica.compras.dto.RecepcionLineaRequest;
import com.logistica.demo.logistica.compras.dto.RecepcionReversionRequest;
import com.logistica.demo.logistica.compras.repository.OrdenCompraRepository;
import com.logistica.demo.logistica.compras.repository.RecepcionAlmacenRepository;
import com.logistica.demo.logistica.inventario.service.InventoryService;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecepcionAlmacenService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final RecepcionAlmacenRepository recepcionRepository;
    private final CurrentUserService currentUserService;
    private final InventoryService inventoryService;

    public RecepcionAlmacenService(
            OrdenCompraRepository ordenCompraRepository,
            RecepcionAlmacenRepository recepcionRepository,
            CurrentUserService currentUserService,
            InventoryService inventoryService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.recepcionRepository = recepcionRepository;
        this.currentUserService = currentUserService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public RecepcionAlmacenResponse registrar(Long ordenCompraId, RecepcionAlmacenRequest request) {
        OrdenCompra ordenCompra = ordenCompraRepository.findById(ordenCompraId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada."));
        if (ordenCompra.getEstado() != EstadoOrdenCompra.APROBADA
                && ordenCompra.getEstado() != EstadoOrdenCompra.PARCIALMENTE_RECIBIDA) {
            throw new BusinessRuleException("Solo se puede recibir una orden APROBADA o PARCIALMENTE_RECIBIDA.");
        }
        if (request == null || request.lineas().isEmpty()) {
            throw new BusinessRuleException("La recepcion debe incluir al menos una linea.");
        }

        Map<Long, OrdenCompraDetalle> detalles = ordenCompra.getDetalles().stream()
                .collect(Collectors.toMap(OrdenCompraDetalle::getId, Function.identity()));
        var requestedIds = new HashSet<Long>();

        RecepcionAlmacen recepcion = new RecepcionAlmacen();
        recepcion.setOrdenCompra(ordenCompra);
        recepcion.setEstado(EstadoRecepcionAlmacen.REGISTRADA);
        recepcion.setReceivedAt(LocalDateTime.now());
        recepcion.setActor(currentUserService.getUsername());

        for (RecepcionLineaRequest linea : request.lineas()) {
            if (linea.ordenCompraDetalleId() == null) {
                throw new BusinessRuleException("La linea de recepcion debe indicar el detalle de OC.");
            }
            if (!requestedIds.add(linea.ordenCompraDetalleId())) {
                throw new BusinessRuleException("No se puede repetir un detalle de OC en la misma recepcion.");
            }
            if (linea.cantidadRecibida() == null || linea.cantidadRecibida() <= 0) {
                throw new BusinessRuleException("La cantidad recibida debe ser mayor a cero.");
            }
            OrdenCompraDetalle detalle = detalles.get(linea.ordenCompraDetalleId());
            if (detalle == null) {
                throw new BusinessRuleException("El detalle de OC no pertenece a la orden indicada.");
            }
            int pendiente = detalle.getCantidad() - detalle.getCantidadRecibida();
            if (linea.cantidadRecibida() > pendiente) {
                throw new BusinessRuleException("La cantidad recibida supera la cantidad pendiente de la OC.");
            }

            detalle.setCantidadRecibida(detalle.getCantidadRecibida() + linea.cantidadRecibida());
            RecepcionAlmacenDetalle recepcionDetalle = new RecepcionAlmacenDetalle();
            recepcionDetalle.setOrdenCompraDetalle(detalle);
            recepcionDetalle.setCantidadRecibida(linea.cantidadRecibida());
            recepcion.addDetalle(recepcionDetalle);
        }

        refreshOrdenStatus(ordenCompra);
        RecepcionAlmacen saved = recepcionRepository.save(recepcion);
        saved.setNumero("REC-%06d".formatted(saved.getId()));
        inventoryService.registerReceipt(saved);
        return mapResponse(saved);
    }

    @Transactional
    public RecepcionAlmacenResponse revertir(Long recepcionId, RecepcionReversionRequest request) {
        RecepcionAlmacen recepcion = recepcionRepository.findById(recepcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Recepcion de almacen no encontrada."));
        if (recepcion.getEstado() != EstadoRecepcionAlmacen.REGISTRADA) {
            throw new BusinessRuleException("Solo se puede revertir una recepcion REGISTRADA.");
        }

        for (RecepcionAlmacenDetalle detalleRecepcion : recepcion.getDetalles()) {
            OrdenCompraDetalle detalle = detalleRecepcion.getOrdenCompraDetalle();
            int nuevaCantidad = detalle.getCantidadRecibida() - detalleRecepcion.getCantidadRecibida();
            if (nuevaCantidad < 0) {
                throw new BusinessRuleException("La reversion dejaria cantidades recibidas negativas.");
            }
            detalle.setCantidadRecibida(nuevaCantidad);
        }

        recepcion.setEstado(EstadoRecepcionAlmacen.REVERTIDA);
        recepcion.setReversedAt(LocalDateTime.now());
        recepcion.setReversedBy(currentUserService.getUsername());
        recepcion.setReversalReason(request != null && request.motivo() != null && !request.motivo().isBlank()
                ? request.motivo().trim()
                : null);
        refreshOrdenStatus(recepcion.getOrdenCompra());
        RecepcionAlmacen saved = recepcionRepository.save(recepcion);
        inventoryService.registerReceiptReversal(saved);
        return mapResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecepcionAlmacenResponse> listarPorOrden(Long ordenCompraId) {
        if (!ordenCompraRepository.existsById(ordenCompraId)) {
            throw new ResourceNotFoundException("Orden de compra no encontrada.");
        }
        return recepcionRepository.findByOrdenCompraIdOrderByReceivedAtDesc(ordenCompraId).stream()
                .map(this::mapResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecepcionAlmacenResponse getById(Long id) {
        return recepcionRepository.findById(id)
                .map(this::mapResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Recepcion de almacen no encontrada."));
    }

    private void refreshOrdenStatus(OrdenCompra ordenCompra) {
        boolean anyReceived = ordenCompra.getDetalles().stream()
                .anyMatch(detalle -> detalle.getCantidadRecibida() > 0);
        boolean fullyReceived = ordenCompra.getDetalles().stream()
                .allMatch(detalle -> detalle.getCantidadRecibida().equals(detalle.getCantidad()));

        if (fullyReceived) {
            ordenCompra.setEstado(EstadoOrdenCompra.RECIBIDA);
        } else if (anyReceived) {
            ordenCompra.setEstado(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA);
        } else {
            ordenCompra.setEstado(EstadoOrdenCompra.APROBADA);
        }
    }

    private RecepcionAlmacenResponse mapResponse(RecepcionAlmacen recepcion) {
        return new RecepcionAlmacenResponse(
                recepcion.getId(),
                recepcion.getNumero(),
                recepcion.getOrdenCompra().getId(),
                recepcion.getOrdenCompra().getNumero(),
                recepcion.getEstado(),
                recepcion.getReceivedAt(),
                recepcion.getActor(),
                recepcion.getReversedAt(),
                recepcion.getReversedBy(),
                recepcion.getReversalReason(),
                recepcion.getDetalles().stream()
                        .map(detalle -> new RecepcionAlmacenDetalleResponse(
                                detalle.getId(),
                                detalle.getOrdenCompraDetalle().getId(),
                                detalle.getOrdenCompraDetalle().getItem().getId(),
                                detalle.getOrdenCompraDetalle().getItem().getCode(),
                                detalle.getOrdenCompraDetalle().getItem().getName(),
                                detalle.getOrdenCompraDetalle().getAlmacen().getId(),
                                detalle.getOrdenCompraDetalle().getAlmacen().getCode(),
                                detalle.getOrdenCompraDetalle().getAlmacen().getName(),
                                detalle.getCantidadRecibida()))
                        .toList(),
                recepcion.getCreatedBy(),
                recepcion.getCreatedAt());
    }
}
