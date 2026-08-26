package com.logistica.demo.maestros.repository;

import com.logistica.demo.maestros.domain.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
