package com.logistica.demo.maestros.service;

import com.logistica.demo.maestros.domain.Almacen;
import com.logistica.demo.maestros.domain.Item;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.maestros.dto.AlmacenCreateRequest;
import com.logistica.demo.maestros.dto.AlmacenResponse;
import com.logistica.demo.maestros.dto.ItemCreateRequest;
import com.logistica.demo.maestros.dto.ItemResponse;
import com.logistica.demo.maestros.dto.ProveedorCreateRequest;
import com.logistica.demo.maestros.dto.ProveedorResponse;
import com.logistica.demo.maestros.repository.AlmacenRepository;
import com.logistica.demo.maestros.repository.ItemRepository;
import com.logistica.demo.maestros.repository.ProveedorRepository;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaestrosCommandService {

    private final ItemRepository itemRepository;
    private final AlmacenRepository almacenRepository;
    private final ProveedorRepository proveedorRepository;

    public MaestrosCommandService(
            ItemRepository itemRepository,
            AlmacenRepository almacenRepository,
            ProveedorRepository proveedorRepository) {
        this.itemRepository = itemRepository;
        this.almacenRepository = almacenRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional
    public ItemResponse createItem(ItemCreateRequest request) {
        String code = normalizeRequired(request.code(), "code");
        String name = normalizeRequired(request.name(), "name");
        String unitMeasure = normalizeRequired(request.unitMeasure(), "unitMeasure");

        if (itemRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Ya existe un item con el codigo '%s'.".formatted(code));
        }

        Item item = new Item();
        item.setCode(code);
        item.setName(name);
        item.setUnitMeasure(unitMeasure);
        item.setActive(true);

        Item saved = itemRepository.save(item);
        return new ItemResponse(saved.getId(), saved.getCode(), saved.getName(), saved.getUnitMeasure());
    }

    @Transactional
    public AlmacenResponse createAlmacen(AlmacenCreateRequest request) {
        String code = normalizeRequired(request.code(), "code");
        String name = normalizeRequired(request.name(), "name");

        if (almacenRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Ya existe un almacen con el codigo '%s'.".formatted(code));
        }

        Almacen almacen = new Almacen();
        almacen.setCode(code);
        almacen.setName(name);
        almacen.setActive(true);

        Almacen saved = almacenRepository.save(almacen);
        return new AlmacenResponse(saved.getId(), saved.getCode(), saved.getName());
    }

    @Transactional
    public ProveedorResponse createProveedor(ProveedorCreateRequest request) {
        String code = normalizeRequired(request.code(), "code");
        String name = normalizeRequired(request.name(), "name");

        if (proveedorRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Ya existe un proveedor con el codigo '%s'.".formatted(code));
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setCode(code);
        proveedor.setName(name);
        proveedor.setActive(true);

        Proveedor saved = proveedorRepository.save(proveedor);
        return new ProveedorResponse(saved.getId(), saved.getCode(), saved.getName());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("El campo '%s' es obligatorio.".formatted(fieldName));
        }
        return value.trim();
    }
}
