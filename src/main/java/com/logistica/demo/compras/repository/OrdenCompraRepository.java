package com.logistica.demo.compras.repository;

import com.logistica.demo.compras.domain.OrdenCompra;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long>, JpaSpecificationExecutor<OrdenCompra> {

    Optional<OrdenCompra> findByRequerimientoId(Long requerimientoId);

    List<OrdenCompra> findAllByOrderByGeneratedAtDesc();

    long countByGeneratedAtBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("""
            select count(oc)
            from OrdenCompra oc
            where oc.generatedAt >= :inicio
              and oc.generatedAt < :fin
              and (:createdBy is null or oc.createdBy = :createdBy)
            """)
    long countGeneratedSince(
            @org.springframework.data.repository.query.Param("inicio") LocalDateTime inicio,
            @org.springframework.data.repository.query.Param("fin") LocalDateTime fin,
            @org.springframework.data.repository.query.Param("createdBy") String createdBy);
}
