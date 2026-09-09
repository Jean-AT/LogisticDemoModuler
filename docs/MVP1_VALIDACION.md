# MVP1 - Criterios de aceptacion y validacion

## Escenario principal de aceptacion

1. Administrador configura compania, ejercicio, periodos, centros, fuentes, metas, clasificadores y bienes.
2. Solicitante registra un Cuadro con programacion mensual y lo envia.
3. Revisor ajusta valores revisados sin alterar los solicitados y aprueba.
4. Consolidador consolida y transfiere una sola vez.
5. Presupuesto genera Unidades, PIA y PIM inicial; el aprobador habilita el ejercicio.
6. Solicitante crea un requerimiento desde una linea transferida con saldo.
7. Aprobador de area aprueba y Presupuesto crea el precompromiso.
8. Compras registra cotizaciones, cierra la comparacion y adjudica proveedor.
9. Compras genera la OC; su aprobacion convierte el precompromiso en compromiso.
10. Almacen registra recepciones parciales y luego completa la orden.
11. El kardex y el stock reflejan exactamente las recepciones.
12. Auditor navega desde la recepcion hasta la orden, requerimiento, control, presupuesto y Cuadro original.

El escenario debe ejecutarse dos veces con datos diferentes y sin intervencion manual en PostgreSQL.

## Casos negativos obligatorios

- Usuario inactivo, token expirado, token revocado y rol sin permiso.
- Acceso a una compania o centro fuera del alcance del usuario.
- Accion sobre periodo cerrado o fuera de la ventana del Cuadro.
- Programacion mensual inconsistente con el total anual.
- Consolidacion con cuadros no revisados.
- Transferencia repetida con la misma o distinta solicitud HTTP.
- Requerimiento por encima del saldo de la linea de Cuadro.
- Precompromiso superior a la disponibilidad.
- Dos aprobaciones concurrentes sobre el mismo saldo.
- Cotizacion adjudicada mientras permanece abierta o sin oferta valida.
- Segunda orden activa para una adjudicacion.
- Orden superior al precompromiso.
- Recepcion superior a la cantidad pendiente.
- Reversion de recepcion que no corresponde al movimiento original.

## Matriz de pruebas

| Nivel | Cobertura minima |
|---|---|
| Dominio | Invariantes, calculos y todas las transiciones validas/invalidas. |
| Aplicacion | Transacciones, permisos, idempotencia y coordinacion entre modulos. |
| Persistencia | Flyway, constraints, locking e indices usando PostgreSQL Testcontainers. |
| API | Contratos `/api/v1`, errores ProblemDetail, paginacion y autorizacion. |
| Arquitectura | Reglas ArchUnit y ausencia de ciclos/dependencias prohibidas. |
| Frontend posterior | Formularios, guards, estados, errores y permisos visibles. |
| E2E backend | Flujo principal y casos negativos mediante API y PostgreSQL. |
| Migracion | Conteos, claves, montos, estados y consulta de documentos `LEGACY_DEMO`. |
| Rendimiento | 150 sesiones, lecturas concurrentes y aprobaciones sobre lineas compartidas. |
| Seguridad | Hash de passwords/tokens, revocacion, scopes, CORS y ausencia de secretos en logs. |

## Umbrales

- Cero saldos presupuestales negativos.
- Cero recepciones superiores a la orden.
- Cero duplicados producidos por reintentos.
- Cero violaciones ArchUnit.
- Cero pruebas backend y E2E de API fallidas en CI; las pruebas frontend se exigiran en su fase.
- `p95 <= 2 s` para CRUD y consultas simples.
- `p95 <= 5 s` para consultas agregadas del MVP.
- Todas las listas grandes paginadas y sin consultas N+1 detectadas.
- Todos los cambios de estado y movimientos criticos auditados.

## Puertas de calidad

Una tarea no se completa hasta que:

1. Sus pruebas unitarias e integracion pasan.
2. Sus migraciones funcionan desde una base vacia y desde la version anterior.
3. OpenAPI y modelos Angular coinciden.
4. Los permisos se validan en backend, no solo en la interfaz.
5. Los criterios del requisito asociado tienen evidencia automatizada o un guion de aceptacion reproducible.

La evidencia automatizada de `ARC-005` es `ModularArchitectureTest`, ejecutada como parte de la suite Maven.

Para `ARC-006`, la suite verifica disponibilidad de Actuator y propagacion del mismo `traceId` entre cabecera y `ProblemDetail`.

Para `ARC-007`, `PostgreSqlMigrationTest` ejecuta la cadena Flyway en PostgreSQL 16 desde una base vacia y desde `V1`; requiere Docker disponible.

Para `PLT-T01`, la misma prueba comprueba la existencia de las 22 tablas de `platform` despues de una instalacion limpia y de una actualizacion incremental. `MigrationCatalogTest` valida siempre que `V4` sea contigua, tenga nombre valido y contenga SQL.

Para `PLT-T02`, `UserAccessProfileTest` cubre union de roles/permisos, alcance por compania, unidad y centro, y rechazo de asignaciones entre companias. `CurrentUserServiceTest` conserva compatibilidad con varias autoridades Spring Security.
