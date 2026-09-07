# MVP1 - Decisiones y alcance

## Decisiones aprobadas

| ID | Decision |
|---|---|
| DEC-001 | El MVP1 se implementara en este mismo repositorio. |
| DEC-002 | Se evolucionara el codigo de forma incremental; no se reescribira desde cero. |
| DEC-003 | El entregable sera una demo funcional integrada, no una salida productiva definitiva. |
| DEC-004 | La arquitectura sera un monolito modular con DDD liviano y limites verificables. |
| DEC-005 | Se conservaran Spring Boot 4, Java 17, Angular 21, PostgreSQL 16, Flyway y Docker. |
| DEC-006 | El modelo quedara preparado para varias companias, aunque la demo configurara una. |
| DEC-007 | Plataforma usara JWT local mejorado, multiples roles y alcances organizacionales. |
| DEC-008 | Las APIs funcionales nuevas se publicaran bajo `/api/v1`; Angular migrara con ellas. |
| DEC-009 | Los datos actuales se preservaran mediante migraciones Flyway controladas. |
| DEC-010 | La demo usara un catalogo representativo y anonimizado, sin conexion directa al ERP legacy. |
| DEC-011 | Cuadro de Necesidades cubrira el flujo anual minimo: registro, programacion mensual, revision, consolidacion y transferencia. |
| DEC-012 | Presupuesto cubrira Presupuesto de Unidades, PIA/PIM minimo, disponibilidad, precompromiso, compromiso y liberacion. |
| DEC-013 | Las aprobaciones del MVP1 tendran etapas y roles fijos; no dependeran de rangos monetarios configurables. |
| DEC-014 | Logistica cubrira bienes, cotizaciones, ordenes de compra, recepcion y almacen. |
| DEC-015 | El proveedor se seleccionara durante la cotizacion/adjudicacion, no al crear el requerimiento. |
| DEC-016 | Las operaciones criticas entre modulos seran sincronas y transaccionales dentro del monolito. |
| DEC-017 | Eventos de dominio y Outbox se reservaran para auditoria, notificaciones e integraciones futuras. |
| DEC-018 | El frontend Angular se compilara junto con el backend para producir un unico despliegue web. |
| DEC-019 | IGV iniciara en 18%, pero se almacenara como parametro vigente y no como constante de negocio. |
| DEC-020 | Zona horaria funcional: `America/Lima`; auditoria persistida como instante. |

## Alcance incluido

### Plataforma

- Autenticacion, renovacion y revocacion de sesiones.
- Usuarios con multiples roles y alcance por compania, unidad y centro de costo.
- Companias, estructura organizacional y maestros fiscales.
- Catalogo corporativo de bienes y unidades de medida.
- Numeracion documental, periodos y auditoria.

### Cuadro de Necesidades

- Ventana anual de registro y revision.
- Necesidades por centro de costo, fuente, meta y bien.
- Programacion mensual.
- Revision, observacion, rechazo y consolidacion.
- Reversion antes de transferir.
- Transferencia idempotente a Presupuesto de Unidades.

### Presupuesto

- Ejercicios `UNIDADES`, `PIA` y `PIM`.
- Techos presupuestales mensuales.
- PIA generado desde el consolidado y PIM inicial igual al PIA aprobado.
- Control de disponibilidad.
- Precompromiso del requerimiento, compromiso de la orden y liberacion de saldo.

### Logistica

- Proveedores y almacenes.
- Requerimientos nacidos desde lineas transferidas del Cuadro.
- Aprobacion de area y presupuesto.
- Cotizaciones, comparacion y adjudicacion.
- Ordenes de compra y aprobacion.
- Recepciones parciales/totales, stock y kardex.
- Consulta y trazabilidad completa.

## Fuera del MVP1

- Cuentas por Pagar y Contabilidad.
- Activo Fijo, Comercial/CxC, GTH, Planillas, Predios, Saneamiento y Juicios.
- Ordenes y conformidades de servicios.
- Inclusiones y exclusiones del Cuadro de Necesidades.
- Planeamiento multianual y carga masiva.
- Modificaciones presupuestales, ejecucion desde vouchers y cierre mensual.
- PAC formal, reposicion automatica y cierre masivo de Logistica.
- SSO corporativo, microservicios, microfrontends, Kubernetes y Event Sourcing.
- Alta disponibilidad productiva y migracion completa de la base legacy.

## Condicion de cierre del MVP1

El alcance se considera terminado cuando un usuario autorizado puede recorrer dos veces el flujo completo, sin cambios manuales en base de datos, sin duplicar documentos y conservando la trazabilidad de cantidades, presupuesto, orden, recepcion y stock.

