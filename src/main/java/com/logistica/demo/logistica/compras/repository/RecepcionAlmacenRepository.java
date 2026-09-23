package com.logistica.demo.logistica.compras.repository;

import com.logistica.demo.logistica.compras.domain.EstadoRecepcionAlmacen;
import com.logistica.demo.logistica.compras.domain.RecepcionAlmacen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecepcionAlmacenRepository extends JpaRepository<RecepcionAlmacen, Long> {

    List<RecepcionAlmacen> findByOrdenCompraIdOrderByReceivedAtDesc(Long ordenCompraId);

    long countByOrdenCompraIdAndEstado(Long ordenCompraId, EstadoRecepcionAlmacen estado);
}
