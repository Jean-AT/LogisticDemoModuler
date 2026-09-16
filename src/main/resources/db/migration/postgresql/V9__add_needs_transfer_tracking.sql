ALTER TABLE cuadronecesidades.needs_consolidations
    ADD COLUMN transfer_id BIGINT,
    ADD COLUMN unit_budget_exercise_id BIGINT;

ALTER TABLE cuadronecesidades.needs_consolidations
    ADD CONSTRAINT uk_needs_consolidation_transfer UNIQUE (transfer_id),
    ADD CONSTRAINT ck_needs_consolidation_transfer_result CHECK (
        status <> 'TRANSFERRED'
        OR (
            transfer_id IS NOT NULL
            AND unit_budget_exercise_id IS NOT NULL
            AND transferred_at IS NOT NULL
        )
    );

CREATE INDEX ix_need_lines_available
    ON cuadronecesidades.need_lines(company_id, id)
    WHERE approved_quantity IS NOT NULL;
