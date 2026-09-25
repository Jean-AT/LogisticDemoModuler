package com.logistica.demo.logistica.inventario.controller;

import com.logistica.demo.logistica.inventario.dto.KardexMovimientoResponse;
import com.logistica.demo.logistica.inventario.dto.StockProjectionResponse;
import com.logistica.demo.logistica.inventario.service.InventoryService;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ApiPaths.LEGACY + "/inventario", ApiPaths.V1 + "/inventario"})
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/kardex")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN', 'APROBADOR')")
    public List<KardexMovimientoResponse> kardex(
            @RequestParam Long itemId,
            @RequestParam Long almacenId) {
        return inventoryService.kardex(itemId, almacenId);
    }

    @GetMapping("/proyeccion")
    @PreAuthorize("hasAnyRole('COMPRAS', 'ADMIN', 'APROBADOR')")
    public StockProjectionResponse projection(
            @RequestParam Long itemId,
            @RequestParam Long almacenId) {
        return inventoryService.projection(itemId, almacenId);
    }
}
