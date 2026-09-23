package com.logistica.demo.logistica.compras.domain;

import com.logistica.demo.logistica.requerimientos.domain.RequerimientoDetalle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cotizacion_proveedor_detalles", schema = "logistica_demo")
public class CotizacionProveedorDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private CotizacionProveedor cotizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requerimiento_detalle_id", nullable = false)
    private RequerimientoDetalle requerimientoDetalle;

    @Column(nullable = false)
    private Integer cantidadOfertada;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalLinea;
}
