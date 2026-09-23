package com.logistica.demo.logistica.compras.domain;

import com.logistica.demo.shared.audit.AuditableEntity;
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
import jakarta.persistence.Table;
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
@Table(name = "recepciones_almacen", schema = "logistica_demo")
public class RecepcionAlmacen extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_compra_id", nullable = false)
    private OrdenCompra ordenCompra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecepcionAlmacen estado = EstadoRecepcionAlmacen.REGISTRADA;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false, length = 100)
    private String actor;

    private LocalDateTime reversedAt;

    @Column(length = 100)
    private String reversedBy;

    @Column(length = 250)
    private String reversalReason;

    @OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecepcionAlmacenDetalle> detalles = new ArrayList<>();

    public void addDetalle(RecepcionAlmacenDetalle detalle) {
        detalle.setRecepcion(this);
        detalles.add(detalle);
    }
}
