package com.logistica.demo.logistica.compras.controller;

import com.logistica.demo.logistica.compras.dto.RecepcionAlmacenRequest;
import com.logistica.demo.logistica.compras.dto.RecepcionAlmacenResponse;
import com.logistica.demo.logistica.compras.dto.RecepcionReversionRequest;
import com.logistica.demo.logistica.compras.service.RecepcionAlmacenService;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ApiPaths.LEGACY + "/recepciones-almacen", ApiPaths.V1 + "/recepciones-almacen"})
public class RecepcionAlmacenController {

    private final RecepcionAlmacenService recepcionService;

    public RecepcionAlmacenController(RecepcionAlmacenService recepcionService) {
        this.recepcionService = recepcionService;
    }

    @PostMapping("/ordenes-compra/{ordenCompraId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public RecepcionAlmacenResponse registrar(
            @PathVariable Long ordenCompraId,
            @RequestBody RecepcionAlmacenRequest request) {
        return recepcionService.registrar(ordenCompraId, request);
    }

    @PostMapping("/{id}/revertir")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public RecepcionAlmacenResponse revertir(
            @PathVariable Long id,
            @RequestBody(required = false) RecepcionReversionRequest request) {
        return recepcionService.revertir(id, request);
    }

    @GetMapping("/ordenes-compra/{ordenCompraId}")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN', 'APROBADOR')")
    public List<RecepcionAlmacenResponse> listarPorOrden(@PathVariable Long ordenCompraId) {
        return recepcionService.listarPorOrden(ordenCompraId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN', 'APROBADOR')")
    public RecepcionAlmacenResponse getById(@PathVariable Long id) {
        return recepcionService.getById(id);
    }
}
