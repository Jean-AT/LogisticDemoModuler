package com.logistica.demo.logistica.compras.domain;

import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.sharedkernel.domain.Moneda;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cotizacion_proveedores", schema = "logistica_demo")
public class CotizacionProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proceso_id", nullable = false)
    private ProcesoCotizacion proceso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacionProveedor estado;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CotizacionProveedorDetalle> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "cotizacion", fetch = FetchType.LAZY)
    private Adjudicacion adjudicacion;

    public void addDetalle(CotizacionProveedorDetalle detalle) {
        detalle.setCotizacion(this);
        detalles.add(detalle);
    }
}
