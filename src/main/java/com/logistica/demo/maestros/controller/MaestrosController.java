package com.logistica.demo.maestros.controller;

import com.logistica.demo.maestros.dto.AlmacenCreateRequest;
import com.logistica.demo.maestros.dto.AlmacenResponse;
import com.logistica.demo.maestros.dto.ItemCreateRequest;
import com.logistica.demo.maestros.dto.ItemResponse;
import com.logistica.demo.maestros.dto.ProveedorCreateRequest;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.maestros.service.MaestrosCommandService;
import com.logistica.demo.maestros.service.MaestrosQueryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MaestrosController {

    private final MaestrosQueryService maestrosQueryService;
    private final MaestrosCommandService maestrosCommandService;

    public MaestrosController(
            MaestrosQueryService maestrosQueryService,
            MaestrosCommandService maestrosCommandService) {
        this.maestrosQueryService = maestrosQueryService;
        this.maestrosCommandService = maestrosCommandService;
    }

    @GetMapping("/items")
    public List<ItemResponse> getItems() {
        return maestrosQueryService.getItems();
    }

    @GetMapping("/almacenes")
    public List<AlmacenResponse> getAlmacenes() {
        return maestrosQueryService.getAlmacenes();
    }

    @GetMapping("/proveedores")
    public List<ProveedorResponse> getProveedores() {
        return maestrosQueryService.getProveedores();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SOLICITANTE')")
    public ItemResponse createItem(@RequestBody ItemCreateRequest request) {
        return maestrosCommandService.createItem(request);
    }

    @PostMapping("/almacenes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SOLICITANTE')")
    public AlmacenResponse createAlmacen(@RequestBody AlmacenCreateRequest request) {
        return maestrosCommandService.createAlmacen(request);
    }

    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SOLICITANTE')")
    public ProveedorResponse createProveedor(@RequestBody ProveedorCreateRequest request) {
        return maestrosCommandService.createProveedor(request);
    }
}
