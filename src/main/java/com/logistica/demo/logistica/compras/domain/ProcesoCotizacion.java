package com.logistica.demo.logistica.compras.domain;

import com.logistica.demo.logistica.requerimientos.domain.Requerimiento;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "cotizacion_procesos", schema = "logistica_demo")
public class ProcesoCotizacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requerimiento_id", nullable = false, unique = true)
    private Requerimiento requerimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProcesoCotizacion estado;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "proceso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CotizacionProveedor> cotizaciones = new ArrayList<>();

    @OneToOne(mappedBy = "proceso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Adjudicacion adjudicacion;

    public void addCotizacion(CotizacionProveedor cotizacion) {
        cotizacion.setProceso(this);
        cotizaciones.add(cotizacion);
    }

    public void setAdjudicacion(Adjudicacion adjudicacion) {
        this.adjudicacion = adjudicacion;
        if (adjudicacion != null) {
            adjudicacion.setProceso(this);
        }
    }
}
