CREATE TABLE logistica_demo.kardex_movimientos (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    source_line_id BIGINT NOT NULL,
    source_number VARCHAR(80) NOT NULL,
    cantidad_delta NUMERIC(18, 4) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kardex_item
        FOREIGN KEY (item_id) REFERENCES logistica_demo.items(id),
    CONSTRAINT fk_kardex_almacen
        FOREIGN KEY (almacen_id) REFERENCES logistica_demo.almacenes(id),
    CONSTRAINT uk_kardex_source_line UNIQUE (source_type, source_line_id),
    CONSTRAINT ck_kardex_tipo
        CHECK (tipo IN ('ENTRADA_RECEPCION', 'REVERSA_RECEPCION')),
    CONSTRAINT ck_kardex_delta_not_zero
        CHECK (cantidad_delta <> 0),
    CONSTRAINT ck_kardex_source_not_blank
        CHECK (
            BTRIM(source_type) <> ''
            AND BTRIM(source_number) <> ''
            AND BTRIM(actor) <> ''
        )
);

CREATE INDEX ix_kardex_item_almacen_fecha
    ON logistica_demo.kardex_movimientos(item_id, almacen_id, occurred_at, id);

CREATE INDEX ix_kardex_source
    ON logistica_demo.kardex_movimientos(source_type, source_id);

CREATE OR REPLACE VIEW logistica_demo.stock_actual AS
SELECT item_id,
       almacen_id,
       SUM(cantidad_delta) AS stock_actual
FROM logistica_demo.kardex_movimientos
GROUP BY item_id, almacen_id;

DO $$
DECLARE
    invalid_kardex_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_kardex_count
    FROM logistica_demo.kardex_movimientos
    WHERE cantidad_delta = 0
       OR source_type IS NULL
       OR source_number IS NULL;

    IF invalid_kardex_count <> 0 THEN
        RAISE EXCEPTION 'LOG-T08 validation failed: % invalid kardex movements',
            invalid_kardex_count;
    END IF;
END $$;
