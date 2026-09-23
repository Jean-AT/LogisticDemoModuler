package com.logistica.demo.logistica.compras.repository;

import com.logistica.demo.logistica.compras.domain.Adjudicacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjudicacionRepository extends JpaRepository<Adjudicacion, Long> {
}
