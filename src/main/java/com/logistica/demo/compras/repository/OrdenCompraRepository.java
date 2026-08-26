package com.logistica.demo.compras.repository;

import com.logistica.demo.compras.domain.OrdenCompra;
import com.logistica.demo.shared.domain.Moneda;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByRequerimientoId(Long requerimientoId);

    List<OrdenCompra> findAllByOrderByGeneratedAtDesc();

    @Query(value = """
            select oc
            from OrdenCompra oc
            where (:numero is null or lower(oc.numero) like lower(concat('%', :numero, '%')))
              and (:proveedorId is null or oc.proveedor.id = :proveedorId)
              and (:moneda is null or oc.moneda = :moneda)
              and (:requerimientoId is null or oc.requerimiento.id = :requerimientoId)
              and (:fechaInicio is null or oc.generatedAt >= :fechaInicio)
              and (:fechaFin is null or oc.generatedAt < :fechaFin)
            """, countQuery = """
            select count(oc)
            from OrdenCompra oc
            where (:numero is null or lower(oc.numero) like lower(concat('%', :numero, '%')))
              and (:proveedorId is null or oc.proveedor.id = :proveedorId)
              and (:moneda is null or oc.moneda = :moneda)
              and (:requerimientoId is null or oc.requerimiento.id = :requerimientoId)
              and (:fechaInicio is null or oc.generatedAt >= :fechaInicio)
              and (:fechaFin is null or oc.generatedAt < :fechaFin)
            """)
    Page<OrdenCompra> search(
            @Param("numero") String numero,
            @Param("proveedorId") Long proveedorId,
            @Param("moneda") Moneda moneda,
            @Param("requerimientoId") Long requerimientoId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    long countByGeneratedAtBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("""
            select count(oc)
            from OrdenCompra oc
            where oc.generatedAt >= :inicio
              and oc.generatedAt < :fin
              and (:createdBy is null or oc.createdBy = :createdBy)
            """)
    long countGeneratedSince(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("createdBy") String createdBy);
}
