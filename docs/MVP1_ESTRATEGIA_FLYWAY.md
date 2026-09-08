# MVP1 - Estrategia Flyway

## Objetivo

La cadena PostgreSQL es incremental, inmutable y reproducible desde una base vacia. La unica ubicacion ejecutada por la aplicacion es `classpath:db/migration/postgresql`.

## Reglas

- `V1-V3` forman el baseline legacy y quedan congeladas; una correccion se publica en una version nueva.
- Cada version tiene un solo responsable funcional y se integra en el orden `V4-V10`.
- No se crean archivos vacios para reservar versiones: Flyway registra checksum y una migracion aplicada no se modifica despues.
- `validate-on-migrate`, validacion de nombres y ubicaciones obligatorias permanecen activos.
- `baseline-on-migrate` y `out-of-order` permanecen desactivados para no ocultar bases incompletas.
- `clean` esta bloqueado en la aplicacion y solo se habilita dentro de pruebas efimeras.
- Los cambios destructivos se dividen en expandir, migrar datos, validar y retirar en una version posterior.
- Cada migracion de datos incluye consultas de comprobacion o constraints que hagan fallar la ejecucion ante inconsistencias.

## Plan V4-V10

| Version | Ticket propietario | Contenido | Validacion minima |
|---|---|---|---|
| `V4` | `PLT-T01` | Esquema `platform`, companias, estructura, maestros fiscales y catalogo. | PK, FK, unicidad por compania y catalogos activos. |
| `V5` | `PLT-T05` | Migracion de usuarios/items legacy y catalogo demo anonimizado. | Conteos, claves legacy unicas y referencias resueltas. |
| `V6` | `CN-T01` | Esquema `cuadro`, planes, lineas, meses, historial y consolidacion. | Mes `1-12`, montos/cantidades no negativos y unicidad anual. |
| `V7` | `PRE-T01` | Esquema `presupuesto`, ejercicios, techos, lineas, movimientos y controles. | Dimensiones completas, movimientos append-only y saldos conciliables. |
| `V8` | `LOG-T02` | Nuevo esquema/modelo `logistica` compatible con Cuadro y Presupuesto. | Relaciones de origen, versiones y numeros documentales unicos. |
| `V9` | `LOG-T02` | Migracion de proveedores, almacenes y documentos actuales como `LEGACY_DEMO`. | Conteos origen/destino, importes y estados conciliados. |
| `V10` | `INT-T02` | Integridad final, indices, vistas y validaciones postmigracion. | Cero huerfanos, duplicados o saldos invalidos; planes de consulta revisados. |

## Validacion automatizada

`PostgreSqlMigrationTest` usa PostgreSQL 16 efimero mediante Testcontainers. Verifica dos rutas:

1. Base vacia hacia la ultima version.
2. Base en `V1` actualizada hacia la ultima version.

La prueba se ejecuta con `mvn test` cuando Docker esta disponible y se omite automaticamente en equipos sin un runtime de contenedores. CI debe disponer de Docker y considerar cualquier omision de esta prueba como fallo de configuracion.
