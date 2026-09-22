ALTER TABLE logistica_demo.requerimientos
    ADD COLUMN budget_control_id BIGINT;

ALTER TABLE logistica_demo.requerimientos
    ADD CONSTRAINT fk_legacy_req_budget_control
        FOREIGN KEY (budget_control_id) REFERENCES presupuesto.budget_controls(id),
    ADD CONSTRAINT uk_legacy_req_budget_control UNIQUE (budget_control_id);

CREATE INDEX ix_legacy_req_budget_control
    ON logistica_demo.requerimientos(budget_control_id);

CREATE OR REPLACE VIEW logistica.requisition_budget_traceability AS
SELECT 'TARGET' AS runtime_model,
       id AS requisition_id,
       number AS requisition_number,
       budget_control_id,
       source_system,
       legacy_id
FROM logistica.requisitions
WHERE budget_control_id IS NOT NULL
UNION ALL
SELECT 'LEGACY_RUNTIME' AS runtime_model,
       id AS requisition_id,
       numero AS requisition_number,
       budget_control_id,
       'MVP1' AS source_system,
       id AS legacy_id
FROM logistica_demo.requerimientos
WHERE budget_control_id IS NOT NULL;

DO $$
DECLARE
    approved_needs_without_precommit_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO approved_needs_without_precommit_count
    FROM logistica_demo.requerimientos
    WHERE needs_line_id IS NOT NULL
      AND estado = 'APROBADO'
      AND budget_control_id IS NULL;

    IF approved_needs_without_precommit_count <> 0 THEN
        RAISE EXCEPTION 'LOG-T04 validation failed: % approved requisitions from needs without budget precommit',
            approved_needs_without_precommit_count;
    END IF;
END $$;
