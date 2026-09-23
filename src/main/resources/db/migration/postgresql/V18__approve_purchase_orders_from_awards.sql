ALTER TABLE logistica_demo.ordenes_compra
    ADD COLUMN adjudicacion_id BIGINT,
    ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'GENERADA',
    ADD COLUMN approved_at TIMESTAMP,
    ADD COLUMN approved_by VARCHAR(100),
    ADD COLUMN budget_control_id BIGINT;

ALTER TABLE logistica_demo.ordenes_compra
    ADD CONSTRAINT fk_oc_adjudicacion
        FOREIGN KEY (adjudicacion_id) REFERENCES logistica_demo.adjudicaciones(id),
    ADD CONSTRAINT uk_oc_adjudicacion UNIQUE (adjudicacion_id),
    ADD CONSTRAINT fk_oc_budget_control
        FOREIGN KEY (budget_control_id) REFERENCES presupuesto.budget_controls(id),
    ADD CONSTRAINT ck_oc_estado
        CHECK (estado IN ('GENERADA', 'APROBADA')),
    ADD CONSTRAINT ck_oc_approval_state
        CHECK (
            (estado = 'GENERADA' AND approved_at IS NULL AND approved_by IS NULL)
            OR (estado = 'APROBADA' AND approved_at IS NOT NULL AND approved_by IS NOT NULL)
        );

ALTER TABLE logistica_demo.orden_compra_detalles
    ADD COLUMN requerimiento_detalle_id BIGINT;

ALTER TABLE logistica_demo.orden_compra_detalles
    ADD CONSTRAINT fk_oc_det_req_detalle
        FOREIGN KEY (requerimiento_detalle_id) REFERENCES logistica_demo.requerimiento_detalles(id);

CREATE INDEX ix_oc_adjudicacion
    ON logistica_demo.ordenes_compra(adjudicacion_id);

CREATE INDEX ix_oc_estado_generated_at
    ON logistica_demo.ordenes_compra(estado, generated_at DESC);

CREATE INDEX ix_oc_det_req_detalle
    ON logistica_demo.orden_compra_detalles(requerimiento_detalle_id);

DO $$
DECLARE
    approved_without_budget_count INTEGER;
    awarded_duplicate_order_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO approved_without_budget_count
    FROM logistica_demo.ordenes_compra oc
    JOIN logistica_demo.requerimientos req ON req.id = oc.requerimiento_id
    WHERE oc.estado = 'APROBADA'
      AND req.needs_line_id IS NOT NULL
      AND oc.budget_control_id IS NULL;

    SELECT COUNT(*) INTO awarded_duplicate_order_count
    FROM (
        SELECT adjudicacion_id
        FROM logistica_demo.ordenes_compra
        WHERE adjudicacion_id IS NOT NULL
        GROUP BY adjudicacion_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF approved_without_budget_count <> 0 OR awarded_duplicate_order_count <> 0 THEN
        RAISE EXCEPTION 'LOG-T06 validation failed: % approved purchase orders without budget control, % duplicated awarded orders',
            approved_without_budget_count,
            awarded_duplicate_order_count;
    END IF;
END $$;
