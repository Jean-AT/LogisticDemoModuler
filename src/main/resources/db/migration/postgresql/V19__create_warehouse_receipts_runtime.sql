ALTER TABLE logistica_demo.orden_compra_detalles
    ADD COLUMN cantidad_recibida INTEGER NOT NULL DEFAULT 0;

ALTER TABLE logistica_demo.orden_compra_detalles
    ADD CONSTRAINT ck_oc_det_cantidad_recibida
        CHECK (cantidad_recibida >= 0 AND cantidad_recibida <= cantidad);

ALTER TABLE logistica_demo.ordenes_compra
    DROP CONSTRAINT ck_oc_estado,
    ADD CONSTRAINT ck_oc_estado
        CHECK (estado IN ('GENERADA', 'APROBADA', 'PARCIALMENTE_RECIBIDA', 'RECIBIDA'));

ALTER TABLE logistica_demo.ordenes_compra
    DROP CONSTRAINT ck_oc_approval_state,
    ADD CONSTRAINT ck_oc_approval_state
        CHECK (
            (estado = 'GENERADA' AND approved_at IS NULL AND approved_by IS NULL)
            OR (estado IN ('APROBADA', 'PARCIALMENTE_RECIBIDA', 'RECIBIDA') AND approved_at IS NOT NULL AND approved_by IS NOT NULL)
        );

CREATE TABLE logistica_demo.recepciones_almacen (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) UNIQUE,
    orden_compra_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    actor VARCHAR(100) NOT NULL,
    reversed_at TIMESTAMP,
    reversed_by VARCHAR(100),
    reversal_reason VARCHAR(250),
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_recepcion_orden_compra
        FOREIGN KEY (orden_compra_id) REFERENCES logistica_demo.ordenes_compra(id),
    CONSTRAINT ck_recepcion_estado
        CHECK (estado IN ('REGISTRADA', 'REVERTIDA')),
    CONSTRAINT ck_recepcion_actor
        CHECK (BTRIM(actor) <> ''),
    CONSTRAINT ck_recepcion_reversion
        CHECK (
            (estado = 'REGISTRADA' AND reversed_at IS NULL AND reversed_by IS NULL)
            OR (estado = 'REVERTIDA' AND reversed_at IS NOT NULL AND reversed_by IS NOT NULL)
        )
);

CREATE TABLE logistica_demo.recepcion_almacen_detalles (
    id BIGSERIAL PRIMARY KEY,
    recepcion_id BIGINT NOT NULL,
    orden_compra_detalle_id BIGINT NOT NULL,
    cantidad_recibida INTEGER NOT NULL,
    CONSTRAINT uk_recepcion_detalle UNIQUE (recepcion_id, orden_compra_detalle_id),
    CONSTRAINT fk_recepcion_detalle_recepcion
        FOREIGN KEY (recepcion_id) REFERENCES logistica_demo.recepciones_almacen(id),
    CONSTRAINT fk_recepcion_detalle_oc_detalle
        FOREIGN KEY (orden_compra_detalle_id) REFERENCES logistica_demo.orden_compra_detalles(id),
    CONSTRAINT ck_recepcion_detalle_cantidad
        CHECK (cantidad_recibida > 0)
);

CREATE INDEX ix_recepciones_orden_estado
    ON logistica_demo.recepciones_almacen(orden_compra_id, estado, received_at DESC);

CREATE INDEX ix_recepcion_detalles_oc_detalle
    ON logistica_demo.recepcion_almacen_detalles(orden_compra_detalle_id);

DO $$
DECLARE
    over_received_count INTEGER;
    inconsistent_received_order_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO over_received_count
    FROM logistica_demo.orden_compra_detalles
    WHERE cantidad_recibida > cantidad;

    SELECT COUNT(*) INTO inconsistent_received_order_count
    FROM logistica_demo.ordenes_compra oc
    WHERE oc.estado IN ('PARCIALMENTE_RECIBIDA', 'RECIBIDA')
      AND NOT EXISTS (
          SELECT 1
          FROM logistica_demo.orden_compra_detalles detail
          WHERE detail.orden_compra_id = oc.id
            AND detail.cantidad_recibida > 0
      );

    IF over_received_count <> 0 OR inconsistent_received_order_count <> 0 THEN
        RAISE EXCEPTION 'LOG-T07 validation failed: % over received lines, % inconsistent received orders',
            over_received_count,
            inconsistent_received_order_count;
    END IF;
END $$;
