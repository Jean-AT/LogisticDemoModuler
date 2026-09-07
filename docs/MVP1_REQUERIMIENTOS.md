# MVP1 - Requerimientos

## Plataforma

| ID | Requerimiento | Criterio funcional |
|---|---|---|
| PLT-RF01 | Autenticar con access/refresh token y permitir revocacion. | Un token expirado o revocado no autoriza solicitudes. |
| PLT-RF02 | Asignar multiples roles y alcances organizacionales. | La API devuelve `403` fuera de la compania/centro autorizado. |
| PLT-RF03 | Mantener companias, unidades y centros de costo. | Los registros usados se desactivan, no se eliminan. |
| PLT-RF04 | Mantener fuentes, metas, actividades y clasificadores. | Solo referencias activas y vigentes pueden utilizarse. |
| PLT-RF05 | Mantener bienes, moneda y unidades de medida. | Cuadro y Logistica consumen el mismo identificador de bien. |
| PLT-RF06 | Administrar periodos y numeracion documental. | No existen numeros duplicados por empresa, ejercicio y tipo. |
| PLT-RF07 | Auditar cambios administrativos y funcionales. | Se identifica actor, instante, accion y documento afectado. |

## Cuadro de Necesidades

| ID | Requerimiento | Criterio funcional |
|---|---|---|
| CN-RF01 | Configurar ventanas de registro, revision y consolidacion. | No se ejecutan acciones fuera de la ventana correspondiente. |
| CN-RF02 | Registrar cabecera por compania, ejercicio, centro, fuente y meta. | El usuario solo usa dimensiones dentro de su alcance. |
| CN-RF03 | Registrar bienes y programacion mensual. | La suma mensual coincide con el total anual. |
| CN-RF04 | Conservar valores solicitados, revisados y aprobados. | La revision no sobrescribe la solicitud original. |
| CN-RF05 | Enviar, observar, revisar o rechazar. | Solo roles y estados validos muestran/aceptan la accion. |
| CN-RF06 | Consolidar cuadros revisados y revertir antes de transferir. | Un cuadro no revisado no entra a la consolidacion. |
| CN-RF07 | Transferir el consolidado a Presupuesto de Unidades. | Reintentar no duplica presupuesto. |
| CN-RF08 | Exponer saldo de cantidades para Logistica. | No se puede consumir mas de lo aprobado. |

## Presupuesto

| ID | Requerimiento | Criterio funcional |
|---|---|---|
| PRE-RF01 | Administrar ejercicios `UNIDADES`, `PIA` y `PIM`. | Solo ejercicios aprobados intervienen en disponibilidad. |
| PRE-RF02 | Configurar techos por mes y dimension fiscal. | Cuadro no aprueba importes sobre el techo vigente. |
| PRE-RF03 | Recibir consolidaciones y crear Presupuesto de Unidades. | Cada transferencia conserva trazabilidad al Cuadro. |
| PRE-RF04 | Generar y aprobar PIA/PIM minimo. | PIM inicial coincide con PIA aprobado. |
| PRE-RF05 | Calcular asignado, precomprometido, comprometido y disponible. | El saldo se explica por movimientos inmutables. |
| PRE-RF06 | Precomprometer un requerimiento aprobado. | Existe un solo precompromiso activo por requerimiento. |
| PRE-RF07 | Comprometer al aprobar la orden. | El precompromiso se convierte de forma atomica. |
| PRE-RF08 | Liberar saldo por rechazo, anulacion o diferencia. | La liberacion nunca supera el saldo del documento origen. |

## Logistica

| ID | Requerimiento | Criterio funcional |
|---|---|---|
| LOG-RF01 | Mantener proveedores y almacenes. | Solo registros activos pueden usarse en nuevos documentos. |
| LOG-RF02 | Crear requerimientos desde lineas transferidas con saldo. | Se heredan dimensiones y se descuenta el saldo solicitado. |
| LOG-RF03 | Aprobar, observar o rechazar el requerimiento. | El historial conserva todas las decisiones. |
| LOG-RF04 | Obtener aprobacion presupuestal y precompromiso. | Sin precompromiso no se inicia compra. |
| LOG-RF05 | Registrar una o mas cotizaciones y adjudicar. | La adjudicacion corresponde a una oferta valida y cerrada. |
| LOG-RF06 | Generar y aprobar orden desde adjudicacion. | No hay OC directa desde requerimiento ni OC duplicada. |
| LOG-RF07 | Calcular subtotal, IGV y total con redondeo definido. | Los valores del servidor coinciden con el detalle. |
| LOG-RF08 | Recibir parcial o totalmente una orden. | La cantidad recibida no supera la pendiente. |
| LOG-RF09 | Mantener kardex y stock por almacen. | Stock equivale a la suma de movimientos completados. |
| LOG-RF10 | Consultar y emitir documentos con trazabilidad. | Desde cada documento se navega hacia su origen y destino. |

## Requerimientos no funcionales

| ID | Requerimiento |
|---|---|
| RNF-01 | Un solo artefacto desplegable para frontend y backend. |
| RNF-02 | Limites modulares verificados automaticamente con ArchUnit. |
| RNF-03 | Migraciones PostgreSQL incrementales y repetibles en un entorno limpio. |
| RNF-04 | Operaciones monetarias y cantidades sin tipos de punto flotante. |
| RNF-05 | Trazabilidad e idempotencia en comandos criticos. |
| RNF-06 | Consultas paginadas, sin N+1 y con indices acordes a filtros. |
| RNF-07 | `p95 <= 2 s` para CRUD y `p95 <= 5 s` para consultas agregadas con carga objetivo. |
| RNF-08 | Logs estructurados con `traceId` y sin datos secretos. |
| RNF-09 | OpenAPI actualizado y errores REST uniformes. |
| RNF-10 | Pruebas con PostgreSQL real mediante Testcontainers y flujo E2E web. |

