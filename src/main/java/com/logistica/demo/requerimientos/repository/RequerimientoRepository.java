package com.logistica.demo.requerimientos.repository;

import com.logistica.demo.requerimientos.domain.EstadoRequerimiento;
import com.logistica.demo.requerimientos.domain.Requerimiento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequerimientoRepository extends JpaRepository<Requerimiento, Long>, JpaSpecificationExecutor<Requerimiento> {

    long countByEstado(EstadoRequerimiento estado);

    long countByEstadoAndCreatedBy(EstadoRequerimiento estado, String createdBy);

    long countByCreatedBy(String createdBy);

    @Query("""
            select count(r)
            from Requerimiento r
            where (:createdBy is null or r.createdBy = :createdBy)
            """)
    long countByOwner(@Param("createdBy") String createdBy);

    @Query("""
            select count(r)
            from Requerimiento r
            where r.estado = :estado
              and (:createdBy is null or r.createdBy = :createdBy)
            """)
    long countByEstadoAndOwner(
            @Param("estado") EstadoRequerimiento estado,
            @Param("createdBy") String createdBy);

    @Query("""
            select count(r)
            from Requerimiento r
            where r.estado = :estado
              and r.updatedAt >= :desde
              and (:createdBy is null or r.createdBy = :createdBy)
            """)
    long countByEstadoSince(
            @Param("estado") EstadoRequerimiento estado,
            @Param("desde") LocalDateTime desde,
            @Param("createdBy") String createdBy);

    @Query("""
            select coalesce(sum(d.subtotalLinea), 0)
            from RequerimientoDetalle d
            where d.requerimiento.estado = :estado
              and (:createdBy is null or d.requerimiento.createdBy = :createdBy)
            """)
    BigDecimal sumDetalleSubtotalByEstado(
            @Param("estado") EstadoRequerimiento estado,
            @Param("createdBy") String createdBy);
}
