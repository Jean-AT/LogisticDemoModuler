# MVP1 - Contratos backend entre modulos

## Reglas publicas

- Solo los paquetes `*.api` forman parte del contrato entre modulos.
- Los contratos usan DTO inmutables, identificadores y value objects del `sharedkernel`.
- Ningun contrato recibe o devuelve entidades JPA, repositorios o clases de infraestructura.
- Los comandos que cambian saldos incluyen `IdempotencyKey` y actor.
- Las listas de comandos son copias inmutables para impedir cambios durante la transaccion.

## Shared Kernel

| Tipo | Uso |
|---|---|
| `Money` | Importe normalizado a dos decimales y moneda obligatoria. |
| `Moneda` | Monedas admitidas inicialmente: PEN y USD. |
| `FiscalDimension` | Compania, ejercicio, mes, centro, fuente, meta y clasificador. |
| `DocumentReference` | Referencia estable a un documento sin acoplar su entidad. |
| `DomainEvent` | Metadatos minimos de eventos: ID, instante, tipo y agregado. |
| `DomainEventPublisher` | Puerto para publicar eventos confirmados por un caso de uso. |
| `OutboxPort` | Puerto para almacenar el evento en la transaccion actual. |
| `IdempotencyPort` | Reserva, completa o libera una ejecucion idempotente. |

Un `IdempotencyClaim` puede quedar en `ACQUIRED`, `IN_PROGRESS` o `COMPLETED`. Una operacion completada conserva codigo HTTP, tipo de contenido y cuerpo para devolver exactamente el resultado original.

## Plataforma

La autenticacion publica estos contratos bajo `/api/auth` y `/api/v1/auth`:

| Metodo | Ruta | Resultado |
|---|---|---|
| `POST` | `/login` | Access token JWT corto, refresh token opaco, expiracion y perfil efectivo. |
| `POST` | `/refresh` | Rota el refresh token y entrega un nuevo par; el token anterior deja de ser valido. |
| `POST` | `/logout` | Revoca el refresh token y responde `204`. |
| `GET` | `/me` | Usuario, roles, permisos y alcances vigentes leidos desde Plataforma. |

HTTP Basic no forma parte del contrato. Los endpoints protegidos aceptan exclusivamente `Authorization: Bearer <accessToken>`.

`PlatformCatalogQuery` publica consultas de referencias activas:

- Compania.
- Centro de costo dentro de una compania.
- Fuente de financiamiento.
- Meta.
- Clasificador de gasto.
- Bien del catalogo corporativo.
- Moneda por codigo.
- Unidad de medida por codigo.
- Bien/servicio por codigo y listado activo por compania, incluyendo unidad y clasificador.

Las ausencias se expresan con `Optional`; el modulo consumidor decide el error de negocio apropiado.

`UserAccessQuery` publica el perfil activo con varias asignaciones `RoleGrant`. Cada asignacion contiene:

- Codigo de rol y conjunto efectivo de permisos.
- Uno o mas alcances por compania, unidad organizacional y centro de costo.
- Semantica jerarquica: compania cubre todas sus unidades; unidad cubre sus centros; centro limita al identificador exacto.

`AccessPolicy.isAllowed` evalua permiso y alcance en una sola operacion, devolviendo `false` para usuarios inactivos, permisos ausentes o dimensiones fuera de alcance.

`UserRoleAdministration` permite asignar/reactivar un rol con varios alcances, revocarlo y reemplazar atomicamente sus permisos. Los contratos normalizan roles y permisos a mayusculas y rechazan alcances de otra compania.

`FiscalPeriodQuery` y `FiscalPeriodAdministration` permiten definir, abrir, cerrar y exigir periodos fiscales abiertos por compania, ejercicio, mes y fecha funcional.

`DocumentSequencePort` entrega numeros documentales transaccionales por compania, ejercicio y tipo, con prefijo configurable y valor incremental bloqueado en base de datos.

`FunctionalAuditPort` registra auditoria funcional append-only con actor, rol efectivo, accion, agregado, `traceId`, IP y cambios serializados.

La superficie administrativa versionada de Plataforma se publica bajo `/api/v1/platform`:

| Metodo | Ruta | Permiso |
|---|---|---|
| `GET` | `/catalog/companies` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/companies/{companyId}` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/cost-centers?companyId=` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/financing-sources?companyId=` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/goals?companyId=&fiscalYear=` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/expense-classifiers?companyId=` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/items?companyId=` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/items/{itemCode}?companyId=` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/currencies/{currencyCode}` | `PLATFORM.MASTER.READ` |
| `GET` | `/catalog/units/{unitCode}` | `PLATFORM.MASTER.READ` |
| `GET` | `/security/users/{username}/access` | `PLATFORM.SECURITY.WRITE` |
| `GET` | `/fiscal-periods?companyId=&date=` o `fiscalYear=&month=` | `PLATFORM.MASTER.READ` |
| `PUT` | `/fiscal-periods` | `PLATFORM.MASTER.WRITE` |
| `POST` | `/fiscal-periods/{companyId}/{fiscalYear}/{month}/open` | `PLATFORM.MASTER.WRITE` |
| `POST` | `/fiscal-periods/{companyId}/{fiscalYear}/{month}/close` | `PLATFORM.MASTER.WRITE` |
| `PUT` | `/document-sequences` | `PLATFORM.MASTER.WRITE` |
| `POST` | `/document-sequences/next` | `PLATFORM.MASTER.WRITE` |

## Cuadro de Necesidades

La superficie versionada de Cuadro se publica bajo `/api/v1/needs`:

| Metodo | Ruta | Uso |
|---|---|---|
| `GET` | `/plans?companyId=&fiscalYear=&status=` | Bandeja de registro/revision por estado. |
| `POST` | `/plans` | Crear Cuadro en borrador con lineas y programacion mensual. |
| `PUT` | `/plans/{id}/details` | Reemplazar lineas durante ventana de registro. |
| `POST` | `/plans/{id}/submit` | Enviar a revision. |
| `POST` | `/plans/{id}/review` | Registrar cantidades revisadas/aprobadas. |
| `POST` | `/plans/{id}/observe` o `/reject` | Observar o rechazar durante revision. |
| `GET` | `/consolidations?companyId=&fiscalYear=` | Bandeja de consolidaciones. |
| `POST` | `/consolidations?companyId=&fiscalYear=` | Consolidar cuadros revisados. |
| `POST` | `/consolidations/{id}/reverse` | Revertir antes de transferencia. |
| `POST` | `/consolidations/{id}/transfer` | Transferir con cabecera `Idempotency-Key`. |
| `GET` | `/balances/{lineId}?companyId=` | Consultar saldo disponible de una linea transferida. |
| `GET` | `/traceability/plans/{id}` | Trazabilidad desde Cuadro hacia consolidacion/transferencia. |

`NeedsBalanceQuery.findAvailableLine(companyId, needsLineId)` devuelve:

- Cuadro y linea origen.
- Compania y ejercicio.
- Centro, fuente, meta y clasificador.
- Bien solicitado.
- Cantidad aprobada, consumida y disponible.
- Disponibilidad desagregada por mes.

Logistica consume este contrato antes de crear o modificar un requerimiento.

`NeedsConsolidationTransferredEvent` informa consolidacion, transferencia, compania y ejercicio. Es informativo; no sustituye la transferencia sincrona.

## Presupuesto

`TransferNeedsToBudgetUseCase.transfer(command)` recibe:

- Consolidacion, compania y ejercicio.
- Lineas del Cuadro con cantidades e importes por dimension mensual.
- Clave de idempotencia y actor.

Devuelve el identificador de transferencia, ejercicio de Unidades, cantidad de lineas y si fue una repeticion atendida desde idempotencia.

La implementacion de Presupuesto tambien publica un adaptador de `NeedsBudgetTransferPort` para que Cuadro invoque la transferencia sin depender de clases internas de Presupuesto. La transferencia crea o reutiliza el ejercicio `UNIDADES`, registra movimientos `ASSIGNMENT` y conserva trazabilidad por linea y mes del Cuadro.

`BudgetPlanUseCase` publica `generatePia`, `reviewPia` y `approvePiaAndCreateInitialPim`. El PIA se genera desde el ejercicio `UNIDADES` aprobado, la revision queda registrada antes de la aprobacion y el PIM inicial se crea con las mismas lineas e importes del PIA aprobado.

`BudgetAvailabilityQuery.findAvailability(dimension, currency)` devuelve asignado, precomprometido, comprometido y disponible.

`BudgetControlUseCase` publica tres comandos:

1. `precommit`: reserva importes para un requerimiento.
2. `commit`: convierte la reserva al aprobar la orden.
3. `release`: libera total o parcialmente con motivo obligatorio en la implementacion.

Cada resultado identifica el control, estado, monto afectado y disponibilidad posterior. `BudgetMovementRegisteredEvent` comunica el movimiento confirmado para auditoria e integraciones futuras.

## Persistencia compartida

Las tablas PostgreSQL de Outbox e idempotencia existen desde `V4`; sus adaptadores transaccionales se implementan en `PLT-T04`, respetando las interfaces aqui definidas.
