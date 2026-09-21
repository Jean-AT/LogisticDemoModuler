ALTER TABLE logistica_demo.requerimientos
    ADD COLUMN company_id BIGINT,
    ADD COLUMN fiscal_year SMALLINT,
    ADD COLUMN needs_plan_id BIGINT,
    ADD COLUMN needs_line_id BIGINT;

ALTER TABLE logistica_demo.requerimiento_detalles
    ADD COLUMN needs_line_id BIGINT,
    ADD COLUMN available_quantity_snapshot NUMERIC(18, 4);

ALTER TABLE logistica_demo.requerimientos
    ADD CONSTRAINT fk_legacy_req_company
        FOREIGN KEY (company_id) REFERENCES platform.companies(id),
    ADD CONSTRAINT fk_legacy_req_needs_plan
        FOREIGN KEY (needs_plan_id) REFERENCES cuadronecesidades.needs_plans(id),
    ADD CONSTRAINT fk_legacy_req_needs_line
        FOREIGN KEY (needs_line_id) REFERENCES cuadronecesidades.need_lines(id),
    ADD CONSTRAINT ck_legacy_req_year
        CHECK (fiscal_year IS NULL OR fiscal_year BETWEEN 2000 AND 2200);

ALTER TABLE logistica_demo.requerimiento_detalles
    ADD CONSTRAINT fk_legacy_req_det_needs_line
        FOREIGN KEY (needs_line_id) REFERENCES cuadronecesidades.need_lines(id),
    ADD CONSTRAINT ck_legacy_req_det_available_quantity
        CHECK (available_quantity_snapshot IS NULL OR available_quantity_snapshot >= 0);

CREATE INDEX ix_legacy_req_needs_line
    ON logistica_demo.requerimientos(needs_line_id);

CREATE INDEX ix_legacy_req_det_needs_line
    ON logistica_demo.requerimiento_detalles(needs_line_id);

CREATE OR REPLACE VIEW logistica.requisition_need_traceability AS
SELECT 'TARGET' AS runtime_model,
       requisition.id AS requisition_id,
       requisition.company_id,
       requisition.fiscal_year,
       line.needs_plan_id,
       line.needs_line_id,
       line.catalog_item_id,
       line.requested_quantity,
       line.source_system,
       line.legacy_id
FROM logistica.requisition_lines line
JOIN logistica.requisitions requisition ON requisition.id = line.requisition_id
WHERE line.needs_line_id IS NOT NULL
UNION ALL
SELECT 'LEGACY_RUNTIME' AS runtime_model,
       legacy.id AS requisition_id,
       legacy.company_id,
       legacy.fiscal_year,
       legacy.needs_plan_id,
       detail.needs_line_id,
       catalog_item.id AS catalog_item_id,
       detail.cantidad::NUMERIC(18, 4) AS requested_quantity,
       'MVP1' AS source_system,
       legacy.id AS legacy_id
FROM logistica_demo.requerimientos legacy
JOIN logistica_demo.requerimiento_detalles detail ON detail.requerimiento_id = legacy.id
JOIN logistica_demo.items item ON item.id = detail.item_id
LEFT JOIN platform.catalog_items catalog_item
  ON catalog_item.company_id = legacy.company_id
 AND catalog_item.code = item.code
WHERE detail.needs_line_id IS NOT NULL;

DO $$
DECLARE
    invalid_legacy_links_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_legacy_links_count
    FROM logistica_demo.requerimiento_detalles detail
    JOIN logistica_demo.requerimientos legacy ON legacy.id = detail.requerimiento_id
    WHERE detail.needs_line_id IS NOT NULL
      AND (
          legacy.company_id IS NULL
          OR legacy.fiscal_year IS NULL
          OR legacy.needs_plan_id IS NULL
          OR legacy.needs_line_id IS NULL
          OR legacy.needs_line_id <> detail.needs_line_id
          OR detail.available_quantity_snapshot IS NULL
      );

    IF invalid_legacy_links_count <> 0 THEN
        RAISE EXCEPTION 'LOG-T03 validation failed: % legacy requisition lines with incomplete needs traceability',
            invalid_legacy_links_count;
    END IF;
END $$;
