package com.logistica.demo.aprobaciones.domain;

import com.logistica.demo.requerimientos.domain.Requerimiento;
import com.logistica.demo.shared.audit.AuditableEntity;
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "aprobaciones", schema = "logistica_demo")
public class Aprobacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requerimiento_id", nullable = false)
    private Requerimiento requerimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionAprobacion accion;

    @Column(length = 250)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime decisionAt;
}
