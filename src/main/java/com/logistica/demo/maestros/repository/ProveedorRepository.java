package com.logistica.demo.maestros.repository;

import com.logistica.demo.maestros.domain.Proveedor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findAllByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
