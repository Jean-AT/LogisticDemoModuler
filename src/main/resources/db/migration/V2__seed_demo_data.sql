INSERT INTO logistica_demo.usuarios (
    username, password, full_name, role, active, created_by, created_at, updated_by, updated_at
) VALUES
    ('solicitante', '{noop}demo123', 'Solicitante Demo', 'SOLICITANTE', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('aprobador', '{noop}demo123', 'Aprobador Demo', 'APROBADOR', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('compras', '{noop}demo123', 'Compras Demo', 'COMPRAS', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('admin', '{noop}demo123', 'Administrador Demo', 'ADMIN', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

INSERT INTO logistica_demo.proveedores (
    code, name, active, created_by, created_at, updated_by, updated_at
) VALUES
    ('PRV-001', 'Proveedor Andino SAC', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('PRV-002', 'Servicios Logisticos del Peru', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

INSERT INTO logistica_demo.almacenes (
    code, name, active, created_by, created_at, updated_by, updated_at
) VALUES
    ('ALM-001', 'Almacen Central', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('ALM-002', 'Almacen Norte', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

INSERT INTO logistica_demo.items (
    code, name, unit_measure, active, created_by, created_at, updated_by, updated_at
) VALUES
    ('ITM-001', 'Mesa operativa', 'UND', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('ITM-002', 'Silla ergonomica', 'UND', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('ITM-003', 'Archivador metalico', 'UND', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);
