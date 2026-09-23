package com.logistica.demo.logistica.compras.controller;

import com.logistica.demo.logistica.compras.dto.AdjudicacionRequest;
import com.logistica.demo.logistica.compras.dto.CotizacionProveedorRequest;
import com.logistica.demo.logistica.compras.dto.ProcesoCotizacionResponse;
import com.logistica.demo.logistica.compras.service.CotizacionService;
import com.logistica.demo.sharedkernel.web.ApiPaths;
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
@RequestMapping({ApiPaths.LEGACY + "/cotizaciones", ApiPaths.V1 + "/cotizaciones"})
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @PostMapping("/procesos/requerimientos/{requerimientoId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public ProcesoCotizacionResponse abrirDesdeRequerimiento(@PathVariable Long requerimientoId) {
        return cotizacionService.abrirDesdeRequerimiento(requerimientoId);
    }

    @PostMapping("/procesos/{procesoId}/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public ProcesoCotizacionResponse registrarCotizacion(
            @PathVariable Long procesoId,
            @RequestBody CotizacionProveedorRequest request) {
        return cotizacionService.registrarCotizacion(procesoId, request);
    }

    @PostMapping("/procesos/{procesoId}/cerrar")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public ProcesoCotizacionResponse cerrar(@PathVariable Long procesoId) {
        return cotizacionService.cerrar(procesoId);
    }

    @PostMapping("/procesos/{procesoId}/adjudicar")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN')")
    public ProcesoCotizacionResponse adjudicar(
            @PathVariable Long procesoId,
            @RequestBody AdjudicacionRequest request) {
        return cotizacionService.adjudicar(procesoId, request);
    }

    @GetMapping("/procesos/{procesoId}")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN', 'APROBADOR')")
    public ProcesoCotizacionResponse getById(@PathVariable Long procesoId) {
        return cotizacionService.getById(procesoId);
    }
}
