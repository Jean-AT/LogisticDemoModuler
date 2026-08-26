package com.logistica.demo.maestros.service;

import com.logistica.demo.maestros.dto.AlmacenResponse;
import com.logistica.demo.maestros.dto.ItemResponse;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.maestros.repository.AlmacenRepository;
import com.logistica.demo.maestros.repository.ItemRepository;
import com.logistica.demo.maestros.repository.ProveedorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaestrosQueryService {

    private final ItemRepository itemRepository;
    private final AlmacenRepository almacenRepository;
    private final ProveedorRepository proveedorRepository;

    public MaestrosQueryService(
            ItemRepository itemRepository,
            AlmacenRepository almacenRepository,
            ProveedorRepository proveedorRepository) {
        this.itemRepository = itemRepository;
        this.almacenRepository = almacenRepository;
        this.proveedorRepository = proveedorRepository;
    }

    public List<ItemResponse> getItems() {
        return itemRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(item -> new ItemResponse(item.getId(), item.getCode(), item.getName(), item.getUnitMeasure()))
                .toList();
    }

    public List<AlmacenResponse> getAlmacenes() {
        return almacenRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(almacen -> new AlmacenResponse(almacen.getId(), almacen.getCode(), almacen.getName()))
                .toList();
    }

    public List<ProveedorResponse> getProveedores() {
        return proveedorRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(proveedor -> new ProveedorResponse(proveedor.getId(), proveedor.getCode(), proveedor.getName()))
                .toList();
    }
}
