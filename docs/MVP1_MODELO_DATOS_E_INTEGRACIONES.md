# MVP1 - Modelo de datos e integraciones

## Esquemas

### `platform`

- `companies`, `organization_units`, `cost_centers`.
- `users`, `roles`, `permissions`, `user_roles`, `user_scopes`, `refresh_tokens`.
- `financing_sources`, `goals`, `activities`, `expense_classifiers`.
- `currencies`, `units_of_measure`, `catalog_items`.
- `fiscal_periods`, `document_sequences`, `audit_events`, `outbox_events`, `idempotency_keys`.

`V4__create_platform_schema.sql` crea estas 22 tablas. Las relaciones organizacionales usan claves compuestas con `company_id` para impedir referencias entre companias. Los maestros empleados por transacciones tienen `active`, auditoria y `version`; los periodos fiscales usan rangos y estados controlados, y los registros de Outbox/auditoria conservan contenido `JSONB`.

`JdbcPlatformCatalogQuery` implementa el puerto publico de consulta sin exponer tablas ni modelos de persistencia. Solo devuelve companias, centros de costo, fuentes, metas, clasificadores y bienes activos; para centros tambien valida su vigencia por fecha.

### `cuadro`

- `needs_plans`: cabecera por compania, ejercicio, centro, fuente y meta.
- `needs_lines`: bien, cantidades y montos solicitados, revisados y aprobados.
- `needs_monthly_schedule`: mes, cantidad y monto para cada linea.
- `needs_status_history`: transiciones, comentario y responsable.
- `needs_consolidations`, `needs_consolidation_members`.
- `needs_budget_transfers`: control idempotente de transferencias.

### `presupuesto`

- `budget_exercises`: tipos `UNIDADES`, `PIA` y `PIM`.
- `budget_ceilings`: techos mensuales por dimensiones fiscales.
- `budget_lines`: asignacion aprobada por compania, ejercicio, mes y dimension.
- `budget_movements`: asignacion, precompromiso, compromiso y liberacion inmutables.
- `budget_controls`, `budget_control_lines`: control asociado al requerimiento u orden.

### `logistica`

- `suppliers`, `warehouses`.
- `requisitions`, `requisition_lines`, `requisition_status_history`.
- `approval_records`.
- `quotation_processes`, `supplier_quotations`, `supplier_quotation_lines`, `awards`.
- `purchase_orders`, `purchase_order_lines`, `purchase_order_status_history`.
- `warehouse_receipts`, `warehouse_receipt_lines`.
- `inventory_movements`, `inventory_movement_lines`.

## Reglas de persistencia

- Todas las tablas transaccionales incluyen `company_id`, auditoria y `version`.
- Los numeros documentales son unicos por compania, ejercicio y tipo.
- La dimension presupuestal minima es compania, ejercicio, mes, centro de costo, fuente, meta y clasificador.
- `UNIDADES` agrega `catalog_item_id`; PIA/PIM se agregan por clasificador y dimensiones fiscales.
- Los documentos conservan snapshots de codigo y descripcion relevantes para no cambiar su historia al editar un maestro.
- Cantidades usan `DECIMAL(18,4)`, precios `DECIMAL(18,4)` y totales `DECIMAL(18,2)`.
- Los movimientos presupuestales y de inventario son append-only.
- No se aplicara particionamiento hasta medir volumen; los indices se definen desde las consultas reales.

## Flujo transaccional

1. Cuadro consolida necesidades revisadas.
2. La transferencia crea lineas `UNIDADES` una sola vez.
3. Presupuesto agrega las lineas y genera PIA; al aprobarlo crea el PIM inicial.
4. Logistica crea un requerimiento desde una linea transferida con saldo.
5. La aprobacion de area solicita control presupuestal.
6. Presupuesto bloquea las lineas afectadas y crea el precompromiso.
7. Logistica cotiza, adjudica y genera la orden.
8. Aprobar la orden convierte el precompromiso en compromiso y libera la diferencia menor.
9. La recepcion genera movimientos de entrada y actualiza la proyeccion de stock.
10. Anulaciones o cierres liberan solamente la parte no comprometida/no recibida.

## Migraciones Flyway previstas

| Version | Objetivo |
|---|---|
| `V4` | Crear esquemas, tipos y tablas base de Plataforma. |
| `V5` | Migrar usuarios e items actuales y cargar catalogo representativo. |
| `V6` | Crear tablas de Cuadro de Necesidades. |
| `V7` | Crear tablas y restricciones de Presupuesto. |
| `V8` | Crear el nuevo modelo de Logistica. |
| `V9` | Migrar proveedores, almacenes, requerimientos, aprobaciones y ordenes actuales. |
| `V10` | Agregar integridad final, indices, vistas de consulta y validaciones postmigracion. |

Los documentos actuales migrados se identificaran como `LEGACY_DEMO`. Podran consultarse, pero no necesitaran inventar una relacion historica con Cuadro o Presupuesto. Todo documento nuevo exigira esas relaciones.

La politica de inmutabilidad, responsables, despliegue y validacion de cada version se detalla en `MVP1_ESTRATEGIA_FLYWAY.md`.

## Integridad e idempotencia

- Una consolidacion tiene como maximo una transferencia efectiva.
- Una linea de requerimiento referencia una linea de Cuadro transferida.
- Un requerimiento tiene como maximo un control presupuestal activo.
- Una adjudicacion genera como maximo una orden activa.
- Una recepcion no puede superar la cantidad pendiente de la orden.
- Revertir una recepcion genera un movimiento opuesto; no elimina el original.
- Los comandos repetidos con la misma clave devuelven el resultado original.
