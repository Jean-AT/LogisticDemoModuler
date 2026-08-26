package com.logistica.demo.maestros.repository;

import com.logistica.demo.maestros.domain.Almacen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {

    List<Almacen> findAllByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
