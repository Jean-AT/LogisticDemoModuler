# MVP1 - Estrategia Flyway

## Objetivo

La cadena PostgreSQL es incremental, inmutable y reproducible desde una base vacia. La unica ubicacion ejecutada por la aplicacion es `classpath:db/migration/postgresql`.

## Reglas

- `V1-V3` forman el baseline legacy y quedan congeladas; una correccion se publica en una version nueva.
- Cada version tiene un solo responsable funcional y se integra en el orden publicado de la cadena `V4+`.
- No se crean archivos vacios para reservar versiones: Flyway registra checksum y una migracion aplicada no se modifica despues.
- `validate-on-migrate`, validacion de nombres y ubicaciones obligatorias permanecen activos.
- `baseline-on-migrate` y `out-of-order` permanecen desactivados para no ocultar bases incompletas.
- `clean` esta bloqueado en la aplicacion y solo se habilita dentro de pruebas efimeras.
- Los cambios destructivos se dividen en expandir, migrar datos, validar y retirar en una version posterior.
- Cada migracion de datos incluye consultas de comprobacion o constraints que hagan fallar la ejecucion ante inconsistencias.

## Plan V4+

| Version | Ticket propietario | Contenido | Validacion minima |
|---|---|---|---|
| `V4` | `PLT-T01` | Esquema `platform`, maestros y tablas base de seguridad, periodos, auditoria e integracion. | 22 tablas, PK, FK, aislamiento por compania, checks e indices operativos. |
| `V5` | `PLT-T03` / `PLT-T05` | Migra anticipadamente usuarios, roles y catalogo minimo requerido por la autenticacion; PLT-T05 completara la validacion funcional del catalogo. | Conteos, claves legacy unicas y referencias resueltas. |
| `V6` | `CN-T01` | Esquema `cuadronecesidades`, planes, lineas y meses. | Mes `1-12`, montos/cantidades no negativos y unicidad anual. |
| `V7` | `CN-T02` | Ventanas y soporte de flujo para Cuadro. | Ventanas validas, tipos controlados e indices activos. |
| `V8` | `CN-T04` | Consolidaciones, origenes y lineas acumuladas. | Unicidad por ejercicio abierto y dimensiones consolidadas. |
| `V9` | `CN-T05` | Resultado de transferencia y saldos disponibles desde Cuadro. | Transferencia trazable y lineas disponibles indexadas. |
| `V10` | `PRE-T01` | Esquema `presupuesto`, ejercicios, techos, lineas y movimientos. | Dimensiones completas, movimientos append-only y saldos conciliables. |
| `V14` | `LOG-T02` | Esquema objetivo `logistica`, maestros logisticos, documentos, cotizaciones, ordenes, recepciones e inventario; migracion legacy como `LEGACY_DEMO`. | Conteos migrados, claves legacy trazables, indices por bandeja y origen. |
| `V15` | `LOG-T03` | Trazabilidad desde requerimientos legacy runtime hacia lineas de Cuadro aprobadas y vista unificada de origen. | FK a Cuadro/Platform, snapshot de saldo disponible e indice por `needs_line_id`. |
| `V16` | `LOG-T04` | Enlace entre requerimientos aprobados y controles presupuestales de precompromiso. | FK unica a `presupuesto.budget_controls`, vista de trazabilidad y bloqueo de aprobados desde Cuadro sin precompromiso. |
| `V17` | `LOG-T05` | Tablas runtime legacy para procesos de cotizacion, ofertas por proveedor, lineas ofertadas y adjudicaciones. | Unicidad por requerimiento/proveedor, checks de estados/importes y validacion de adjudicaciones consistentes. |

## Validacion automatizada

`PostgreSqlMigrationTest` usa PostgreSQL 16 efimero mediante Testcontainers. Verifica dos rutas:

1. Base vacia hacia la ultima version.
2. Base en `V1` actualizada hacia la ultima version.

La prueba se ejecuta con `mvn test` cuando Docker esta disponible y se omite automaticamente en equipos sin un runtime de contenedores. CI debe disponer de Docker y considerar cualquier omision de esta prueba como fallo de configuracion.
