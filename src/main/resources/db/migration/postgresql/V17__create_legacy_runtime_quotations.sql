CREATE TABLE logistica_demo.cotizacion_procesos (
    id BIGSERIAL PRIMARY KEY,
    requerimiento_id BIGINT NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cotizacion_proceso_requerimiento
        FOREIGN KEY (requerimiento_id) REFERENCES logistica_demo.requerimientos(id),
    CONSTRAINT ck_cotizacion_proceso_estado
        CHECK (estado IN ('ABIERTO', 'CERRADO', 'ADJUDICADO', 'CANCELADO')),
    CONSTRAINT ck_cotizacion_proceso_cierre
        CHECK (closed_at IS NULL OR closed_at >= opened_at)
);

CREATE TABLE logistica_demo.cotizacion_proveedores (
    id BIGSERIAL PRIMARY KEY,
    proceso_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    total DECIMAL(18, 2) NOT NULL,
    CONSTRAINT uk_cotizacion_proveedor UNIQUE (proceso_id, proveedor_id),
    CONSTRAINT fk_cotizacion_proveedor_proceso
        FOREIGN KEY (proceso_id) REFERENCES logistica_demo.cotizacion_procesos(id),
    CONSTRAINT fk_cotizacion_proveedor_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES logistica_demo.proveedores(id),
    CONSTRAINT ck_cotizacion_proveedor_estado
        CHECK (estado IN ('PRESENTADA', 'DESCALIFICADA', 'ADJUDICADA')),
    CONSTRAINT ck_cotizacion_proveedor_total
        CHECK (total >= 0)
);

CREATE TABLE logistica_demo.cotizacion_proveedor_detalles (
    id BIGSERIAL PRIMARY KEY,
    cotizacion_id BIGINT NOT NULL,
    requerimiento_detalle_id BIGINT NOT NULL,
    cantidad_ofertada INT NOT NULL,
    precio_unitario DECIMAL(18, 2) NOT NULL,
    subtotal_linea DECIMAL(18, 2) NOT NULL,
    CONSTRAINT uk_cotizacion_proveedor_detalle UNIQUE (cotizacion_id, requerimiento_detalle_id),
    CONSTRAINT fk_cotizacion_detalle_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES logistica_demo.cotizacion_proveedores(id),
    CONSTRAINT fk_cotizacion_detalle_req_detalle
        FOREIGN KEY (requerimiento_detalle_id) REFERENCES logistica_demo.requerimiento_detalles(id),
    CONSTRAINT ck_cotizacion_detalle_amounts
        CHECK (cantidad_ofertada > 0 AND precio_unitario >= 0 AND subtotal_linea >= 0)
);

CREATE TABLE logistica_demo.adjudicaciones (
    id BIGSERIAL PRIMARY KEY,
    proceso_id BIGINT NOT NULL UNIQUE,
    cotizacion_id BIGINT NOT NULL UNIQUE,
    awarded_at TIMESTAMP NOT NULL,
    actor VARCHAR(100) NOT NULL,
    CONSTRAINT fk_adjudicacion_proceso
        FOREIGN KEY (proceso_id) REFERENCES logistica_demo.cotizacion_procesos(id),
    CONSTRAINT fk_adjudicacion_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES logistica_demo.cotizacion_proveedores(id),
    CONSTRAINT ck_adjudicacion_actor
        CHECK (BTRIM(actor) <> '')
);

CREATE INDEX ix_cotizacion_procesos_estado
    ON logistica_demo.cotizacion_procesos(estado, opened_at DESC);

CREATE INDEX ix_cotizacion_proveedores_proceso_estado
    ON logistica_demo.cotizacion_proveedores(proceso_id, estado);

DO $$
DECLARE
    awarded_open_process_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO awarded_open_process_count
    FROM logistica_demo.adjudicaciones award
    JOIN logistica_demo.cotizacion_procesos process ON process.id = award.proceso_id
    JOIN logistica_demo.cotizacion_proveedores quote ON quote.id = award.cotizacion_id
    WHERE process.estado <> 'ADJUDICADO'
       OR quote.estado <> 'ADJUDICADA'
       OR quote.proceso_id <> process.id;

    IF awarded_open_process_count <> 0 THEN
        RAISE EXCEPTION 'LOG-T05 validation failed: % inconsistent awarded quotations',
            awarded_open_process_count;
    END IF;
END $$;
