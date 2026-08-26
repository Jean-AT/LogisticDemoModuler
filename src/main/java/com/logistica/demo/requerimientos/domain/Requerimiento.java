package com.logistica.demo.requerimientos.domain;

import com.logistica.demo.aprobaciones.domain.Aprobacion;
import com.logistica.demo.compras.domain.OrdenCompra;
import com.logistica.demo.maestros.domain.Proveedor;
import com.logistica.demo.shared.audit.AuditableEntity;
import com.logistica.demo.shared.domain.Moneda;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "requerimientos", schema = "logistica_demo")
public class Requerimiento extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false, length = 250)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRequerimiento estado;

    @OneToMany(mappedBy = "requerimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequerimientoDetalle> detalles = new ArrayList<>();

    @OrderBy("decisionAt asc")
    @OneToMany(mappedBy = "requerimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aprobacion> aprobaciones = new ArrayList<>();

    @OrderBy("createdAt asc")
    @OneToMany(mappedBy = "requerimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequerimientoEstadoHistorial> historialEstados = new ArrayList<>();

    @OneToOne(mappedBy = "requerimiento", fetch = FetchType.LAZY)
    private OrdenCompra ordenCompra;

    public void replaceDetalles(List<RequerimientoDetalle> nuevosDetalles) {
        detalles.clear();
        nuevosDetalles.forEach(this::addDetalle);
    }

    public void addDetalle(RequerimientoDetalle detalle) {
        detalle.setRequerimiento(this);
        detalles.add(detalle);
    }

    public void addAprobacion(Aprobacion aprobacion) {
        aprobacion.setRequerimiento(this);
        aprobaciones.add(aprobacion);
    }

    public void cambiarEstado(EstadoRequerimiento nuevoEstado, String comentario) {
        RequerimientoEstadoHistorial historial = new RequerimientoEstadoHistorial();
        historial.setEstadoAnterior(estado);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setComentario(comentario == null || comentario.isBlank() ? null : comentario.trim());
        addHistorialEstado(historial);
        estado = nuevoEstado;
    }

    public void addHistorialEstado(RequerimientoEstadoHistorial historial) {
        historial.setRequerimiento(this);
        historialEstados.add(historial);
    }
}
