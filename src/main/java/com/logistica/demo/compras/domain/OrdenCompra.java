package com.logistica.demo.compras.domain;

import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.requerimientos.domain.Requerimiento;
import com.logistica.demo.shared.audit.AuditableEntity;
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
@Table(name = "ordenes_compra", schema = "logistica_demo")
public class OrdenCompra extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requerimiento_id", nullable = false, unique = true)
    private Requerimiento requerimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal tipoCambio;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenCompraDetalle> detalles = new ArrayList<>();

    public void addDetalle(OrdenCompraDetalle detalle) {
        detalle.setOrdenCompra(this);
        detalles.add(detalle);
    }
}
