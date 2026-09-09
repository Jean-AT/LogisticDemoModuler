INSERT INTO platform.companies (
    code, legal_name, trade_name, tax_id, timezone, active, created_by, updated_by
) VALUES (
    'DEMO', 'Logistica Demo SAC', 'Logistica Demo', '00000000000', 'America/Lima', TRUE, 'system', 'system'
);

INSERT INTO platform.roles (code, name, active, system_role, created_by, updated_by) VALUES
    ('SOLICITANTE', 'Solicitante', TRUE, TRUE, 'system', 'system'),
    ('APROBADOR', 'Aprobador', TRUE, TRUE, 'system', 'system'),
    ('COMPRAS', 'Compras', TRUE, TRUE, 'system', 'system'),
    ('ADMIN', 'Administrador', TRUE, TRUE, 'system', 'system');

INSERT INTO platform.permissions (code, name, module, active) VALUES
    ('PLATFORM.MASTER.READ', 'Consultar maestros', 'PLATFORM', TRUE),
    ('PLATFORM.MASTER.WRITE', 'Administrar maestros', 'PLATFORM', TRUE),
    ('PLATFORM.SECURITY.WRITE', 'Administrar seguridad', 'PLATFORM', TRUE),
    ('CUADRO.READ', 'Consultar cuadros', 'CUADRO', TRUE),
    ('CUADRO.WRITE', 'Registrar cuadros', 'CUADRO', TRUE),
    ('CUADRO.APPROVE', 'Revisar y aprobar cuadros', 'CUADRO', TRUE),
    ('PRESUPUESTO.READ', 'Consultar presupuesto', 'PRESUPUESTO', TRUE),
    ('PRESUPUESTO.CONTROL', 'Ejecutar control presupuestal', 'PRESUPUESTO', TRUE),
    ('LOGISTICA.REQUISITION.WRITE', 'Registrar requerimientos', 'LOGISTICA', TRUE),
    ('LOGISTICA.REQUISITION.APPROVE', 'Aprobar requerimientos', 'LOGISTICA', TRUE),
    ('LOGISTICA.PURCHASE.WRITE', 'Gestionar compras', 'LOGISTICA', TRUE);

INSERT INTO platform.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM platform.roles r
JOIN platform.permissions p ON
       r.code = 'ADMIN'
    OR (r.code = 'SOLICITANTE' AND p.code IN (
        'PLATFORM.MASTER.READ', 'CUADRO.READ', 'CUADRO.WRITE', 'LOGISTICA.REQUISITION.WRITE'
    ))
    OR (r.code = 'APROBADOR' AND p.code IN (
        'PLATFORM.MASTER.READ', 'CUADRO.READ', 'CUADRO.APPROVE',
        'PRESUPUESTO.READ', 'LOGISTICA.REQUISITION.APPROVE'
    ))
    OR (r.code = 'COMPRAS' AND p.code IN (
        'PLATFORM.MASTER.READ', 'PRESUPUESTO.READ', 'LOGISTICA.PURCHASE.WRITE'
    ));

INSERT INTO platform.users (
    username, password_hash, full_name, active, created_by, updated_by
)
SELECT
    username,
    '$2a$12$QOAwHgrBpS/bbsdZSr.6heedCQi03lEPAMvUeirYvjP/wkmMeR1pK',
    full_name,
    active,
    'system',
    'system'
FROM logistica_demo.usuarios;

UPDATE logistica_demo.usuarios
SET password = '$2a$12$QOAwHgrBpS/bbsdZSr.6heedCQi03lEPAMvUeirYvjP/wkmMeR1pK',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO platform.user_roles (
    user_id, role_id, company_id, active, valid_from, created_by
)
SELECT u.id, r.id, c.id, TRUE, CURRENT_TIMESTAMP, 'system'
FROM platform.users u
JOIN logistica_demo.usuarios legacy_user ON legacy_user.username = u.username
JOIN platform.roles r ON r.code = legacy_user.role
CROSS JOIN platform.companies c
WHERE c.code = 'DEMO';

INSERT INTO platform.user_scopes (
    user_role_id, company_id, organization_unit_id, cost_center_id, created_by
)
SELECT ur.id, ur.company_id, NULL, NULL, 'system'
FROM platform.user_roles ur;

INSERT INTO platform.currencies (
    code, name, symbol, decimal_places, active, created_by, updated_by
) VALUES
    ('PEN', 'Sol peruano', 'S/', 2, TRUE, 'system', 'system'),
    ('USD', 'Dolar estadounidense', '$', 2, TRUE, 'system', 'system');

INSERT INTO platform.units_of_measure (
    code, name, active, created_by, updated_by
) VALUES
    ('UND', 'Unidad', TRUE, 'system', 'system');

INSERT INTO platform.catalog_items (
    company_id, unit_of_measure_id, code, name, item_type, active, created_by, updated_by
)
SELECT
    c.id,
    uom.id,
    legacy_item.code,
    legacy_item.name,
    'GOOD',
    legacy_item.active,
    'system',
    'system'
FROM logistica_demo.items legacy_item
CROSS JOIN platform.companies c
CROSS JOIN platform.units_of_measure uom
WHERE c.code = 'DEMO'
  AND uom.code = 'UND';
