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

INSERT INTO platform.organization_units (
    company_id, parent_id, code, name, unit_type, active, created_by, updated_by
)
SELECT c.id, NULL, 'GER-GRAL', 'Gerencia General', 'MANAGEMENT', TRUE, 'system', 'system'
FROM platform.companies c
WHERE c.code = 'DEMO';

INSERT INTO platform.organization_units (
    company_id, parent_id, code, name, unit_type, active, created_by, updated_by
)
SELECT c.id, parent.id, child.code, child.name, child.unit_type, TRUE, 'system', 'system'
FROM platform.companies c
JOIN platform.organization_units parent
  ON parent.company_id = c.id
 AND parent.code = 'GER-GRAL'
CROSS JOIN (
    VALUES
        ('ADM', 'Administracion y Finanzas', 'ADMINISTRATION'),
        ('LOG', 'Logistica y Abastecimiento', 'OPERATIONS')
) AS child(code, name, unit_type)
WHERE c.code = 'DEMO';

INSERT INTO platform.cost_centers (
    company_id, organization_unit_id, code, name, valid_from, active, created_by, updated_by
)
SELECT c.id, ou.id, seed.code, seed.name, DATE '2026-01-01', TRUE, 'system', 'system'
FROM platform.companies c
JOIN platform.organization_units ou ON ou.company_id = c.id
JOIN (
    VALUES
        ('ADM', 'CC-ADM', 'Centro de costo Administracion'),
        ('LOG', 'CC-LOG', 'Centro de costo Logistica')
) AS seed(unit_code, code, name) ON seed.unit_code = ou.code
WHERE c.code = 'DEMO';

INSERT INTO platform.financing_sources (
    company_id, code, name, active, created_by, updated_by
)
SELECT c.id, seed.code, seed.name, TRUE, 'system', 'system'
FROM platform.companies c
CROSS JOIN (
    VALUES
        ('RO', 'Recursos ordinarios'),
        ('RDR', 'Recursos directamente recaudados')
) AS seed(code, name)
WHERE c.code = 'DEMO';

INSERT INTO platform.goals (
    company_id, fiscal_year, code, name, active, created_by, updated_by
)
SELECT c.id, 2026, seed.code, seed.name, TRUE, 'system', 'system'
FROM platform.companies c
CROSS JOIN (
    VALUES
        ('META-001', 'Gestion administrativa institucional'),
        ('META-002', 'Abastecimiento oportuno de bienes y servicios')
) AS seed(code, name)
WHERE c.code = 'DEMO';

INSERT INTO platform.activities (
    company_id, fiscal_year, code, name, active, created_by, updated_by
)
SELECT c.id, 2026, seed.code, seed.name, TRUE, 'system', 'system'
FROM platform.companies c
CROSS JOIN (
    VALUES
        ('ACT-001', 'Operacion administrativa'),
        ('ACT-002', 'Gestion de compras y almacenes')
) AS seed(code, name)
WHERE c.code = 'DEMO';

INSERT INTO platform.expense_classifiers (
    company_id, code, name, active, created_by, updated_by
)
SELECT c.id, seed.code, seed.name, TRUE, 'system', 'system'
FROM platform.companies c
CROSS JOIN (
    VALUES
        ('2.3.1.5.1.2', 'Papeleria en general, utiles y materiales de oficina'),
        ('2.3.2.7.11.99', 'Servicios diversos'),
        ('2.6.3.2.1.2', 'Mobiliario y equipos de oficina')
) AS seed(code, name)
WHERE c.code = 'DEMO';

INSERT INTO platform.currencies (
    code, name, symbol, decimal_places, active, created_by, updated_by
) VALUES
    ('PEN', 'Sol peruano', 'S/', 2, TRUE, 'system', 'system'),
    ('USD', 'Dolar estadounidense', '$', 2, TRUE, 'system', 'system');

INSERT INTO platform.units_of_measure (
    code, name, active, created_by, updated_by
) VALUES
    ('UND', 'Unidad', TRUE, 'system', 'system'),
    ('CAJA', 'Caja', TRUE, 'system', 'system'),
    ('SERV', 'Servicio', TRUE, 'system', 'system');

INSERT INTO platform.catalog_items (
    company_id, unit_of_measure_id, expense_classifier_id, code, name, item_type, active, created_by, updated_by
)
SELECT
    c.id,
    uom.id,
    classifier.id,
    legacy_item.code,
    legacy_item.name,
    'GOOD',
    legacy_item.active,
    'system',
    'system'
FROM logistica_demo.items legacy_item
CROSS JOIN platform.companies c
CROSS JOIN platform.units_of_measure uom
JOIN platform.expense_classifiers classifier
  ON classifier.company_id = c.id
 AND classifier.code = '2.6.3.2.1.2'
WHERE c.code = 'DEMO'
  AND uom.code = 'UND';

INSERT INTO platform.catalog_items (
    company_id, unit_of_measure_id, expense_classifier_id, code, name, item_type, active, created_by, updated_by
)
SELECT c.id, uom.id, classifier.id, seed.code, seed.name, seed.item_type, TRUE, 'system', 'system'
FROM platform.companies c
JOIN (
    VALUES
        ('ITM-004', 'Papel bond A4 75g', 'GOOD', 'CAJA', '2.3.1.5.1.2'),
        ('SERV-001', 'Mantenimiento preventivo de mobiliario', 'SERVICE', 'SERV', '2.3.2.7.11.99')
) AS seed(code, name, item_type, unit_code, classifier_code) ON TRUE
JOIN platform.units_of_measure uom ON uom.code = seed.unit_code
JOIN platform.expense_classifiers classifier
  ON classifier.company_id = c.id
 AND classifier.code = seed.classifier_code
WHERE c.code = 'DEMO';

DO $$
DECLARE
    legacy_users_count INTEGER;
    platform_users_count INTEGER;
    legacy_items_count INTEGER;
    migrated_items_count INTEGER;
    unresolved_user_roles_count INTEGER;
    catalog_without_classifier_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO legacy_users_count FROM logistica_demo.usuarios;
    SELECT COUNT(*) INTO platform_users_count FROM platform.users;
    SELECT COUNT(*) INTO legacy_items_count FROM logistica_demo.items;
    SELECT COUNT(*) INTO migrated_items_count
    FROM platform.catalog_items catalog_item
    JOIN logistica_demo.items legacy_item ON legacy_item.code = catalog_item.code;
    SELECT COUNT(*) INTO unresolved_user_roles_count
    FROM logistica_demo.usuarios legacy_user
    LEFT JOIN platform.users platform_user ON platform_user.username = legacy_user.username
    LEFT JOIN platform.user_roles user_role ON user_role.user_id = platform_user.id
    WHERE user_role.id IS NULL;
    SELECT COUNT(*) INTO catalog_without_classifier_count
    FROM platform.catalog_items
    WHERE expense_classifier_id IS NULL;

    IF platform_users_count <> legacy_users_count THEN
        RAISE EXCEPTION 'PLT-T05 validation failed: migrated users %, expected %',
            platform_users_count, legacy_users_count;
    END IF;
    IF migrated_items_count <> legacy_items_count THEN
        RAISE EXCEPTION 'PLT-T05 validation failed: migrated catalog items %, expected %',
            migrated_items_count, legacy_items_count;
    END IF;
    IF unresolved_user_roles_count <> 0 THEN
        RAISE EXCEPTION 'PLT-T05 validation failed: % users without platform role',
            unresolved_user_roles_count;
    END IF;
    IF catalog_without_classifier_count <> 0 THEN
        RAISE EXCEPTION 'PLT-T05 validation failed: % catalog items without classifier',
            catalog_without_classifier_count;
    END IF;
END $$;
