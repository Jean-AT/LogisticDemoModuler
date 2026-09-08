# MVP1 - Backlog ordenado

Las tareas se ejecutan en el orden indicado. Una tarea posterior puede comenzar solo cuando sus dependencias esten `COMPLETADA`.

## Fase 0 - Fundamentos

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 001 | `DOC-001` Crear documentacion de decisiones, arquitectura, datos, requisitos, backlog y validacion. | Todos | - | COMPLETADA |
| 002 | `ARC-001` Crear espacios de paquetes para los cinco modulos objetivo. | RNF-02 | DOC-001 | COMPLETADA |
| 003 | `ARC-002` Confirmar build backend independiente y diferir frontend hasta estabilizar la API. | RNF-01 | ARC-001 | COMPLETADA |
| 004 | `ARC-003` Introducir `/api/v1`, `ProblemDetail`, paginacion y tipos comunes. | RNF-04, RNF-09 | ARC-001 | COMPLETADA |
| 005 | `ARC-004` Definir contratos de modulo, eventos, Outbox e idempotencia. | RNF-02, RNF-05 | ARC-003 | COMPLETADA |
| 006 | `ARC-005` Agregar ArchUnit y reglas de dependencia. | RNF-02 | ARC-004 | COMPLETADA |
| 007 | `ARC-006` Incorporar Actuator, perfiles y logs con `traceId`. | RNF-08 | ARC-003 | COMPLETADA |
| 008 | `ARC-007` Crear estrategia Flyway `V4-V10` y validacion de migraciones. | RNF-03 | ARC-004 | PENDIENTE |

## Fase 1 - Plataforma

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 101 | `PLT-T01` Crear esquema `platform` y maestros organizacionales/fiscales. | PLT-RF03, PLT-RF04, PLT-RF05 | ARC-007 | PENDIENTE |
| 102 | `PLT-T02` Implementar roles multiples, permisos y alcances. | PLT-RF02 | PLT-T01 | PENDIENTE |
| 103 | `PLT-T03` Reemplazar JWT manual, `{noop}` y HTTP Basic; agregar refresh/revocacion. | PLT-RF01 | PLT-T02 | PENDIENTE |
| 104 | `PLT-T04` Implementar periodos, secuencias, auditoria y Outbox. | PLT-RF06, PLT-RF07 | PLT-T01, ARC-004 | PENDIENTE |
| 105 | `PLT-T05` Migrar usuarios e items actuales y cargar catalogo representativo. | PLT-RF01, PLT-RF05 | PLT-T03, PLT-T04 | PENDIENTE |
| 106 | `PLT-T06` Crear APIs `/api/v1/platform` y pantallas administrativas. | PLT-RF02-PLT-RF06 | PLT-T05 | PENDIENTE |
| 107 | `PLT-T07` Agregar pruebas de seguridad, alcance, maestros y migracion. | PLT-RF01-PLT-RF07 | PLT-T06 | PENDIENTE |

## Fase 2 - Cuadro de Necesidades

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 201 | `CN-T01` Crear esquema y agregados de Cuadro, detalle y programacion mensual. | CN-RF01-CN-RF04 | PLT-T07 | PENDIENTE |
| 202 | `CN-T02` Implementar estados, ventanas y reglas de edicion. | CN-RF01, CN-RF05 | CN-T01 | PENDIENTE |
| 203 | `CN-T03` Implementar revision conservando solicitado/revisado/aprobado. | CN-RF04, CN-RF05 | CN-T02 | PENDIENTE |
| 204 | `CN-T04` Implementar consolidacion y reversion controlada. | CN-RF06 | CN-T03 | PENDIENTE |
| 205 | `CN-T05` Implementar transferencia idempotente y consulta de saldos. | CN-RF07, CN-RF08 | CN-T04, ARC-004 | PENDIENTE |
| 206 | `CN-T06` Crear pantallas de registro, revision, consolidacion y trazabilidad. | CN-RF01-CN-RF08 | CN-T05 | PENDIENTE |
| 207 | `CN-T07` Agregar pruebas unitarias, API e integracion del flujo anual. | CN-RF01-CN-RF08 | CN-T06 | PENDIENTE |

## Fase 3 - Presupuesto

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 301 | `PRE-T01` Crear ejercicios, techos, lineas y movimientos presupuestales. | PRE-RF01, PRE-RF02, PRE-RF05 | CN-T07 | PENDIENTE |
| 302 | `PRE-T02` Recibir transferencia y generar Presupuesto de Unidades. | PRE-RF03 | PRE-T01 | PENDIENTE |
| 303 | `PRE-T03` Generar, revisar y aprobar PIA/PIM inicial. | PRE-RF04 | PRE-T02 | PENDIENTE |
| 304 | `PRE-T04` Implementar disponibilidad con control de concurrencia. | PRE-RF05 | PRE-T03 | PENDIENTE |
| 305 | `PRE-T05` Implementar `BudgetControlUseCase`. | PRE-RF06-PRE-RF08 | PRE-T04 | PENDIENTE |
| 306 | `PRE-T06` Crear pantallas de ejercicios, control y consultas. | PRE-RF01-PRE-RF08 | PRE-T05 | PENDIENTE |
| 307 | `PRE-T07` Probar saldos, idempotencia y aprobaciones concurrentes. | PRE-RF01-PRE-RF08 | PRE-T06 | PENDIENTE |

## Fase 4 - Logistica integrada

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 401 | `LOG-T01` Reubicar codigo actual bajo el modulo Logistica sin cambiar comportamiento. | RNF-02 | PRE-T07 | PENDIENTE |
| 402 | `LOG-T02` Crear nuevo modelo y migrar datos actuales como `LEGACY_DEMO`. | LOG-RF01-LOG-RF03 | LOG-T01, ARC-007 | PENDIENTE |
| 403 | `LOG-T03` Crear requerimiento desde linea de Cuadro y controlar cantidad disponible. | LOG-RF02 | LOG-T02 | PENDIENTE |
| 404 | `LOG-T04` Integrar aprobacion de area y precompromiso. | LOG-RF03, LOG-RF04 | LOG-T03, PRE-T05 | PENDIENTE |
| 405 | `LOG-T05` Implementar cotizaciones, comparacion y adjudicacion. | LOG-RF05 | LOG-T04 | PENDIENTE |
| 406 | `LOG-T06` Generar y aprobar OC desde adjudicacion, comprometiendo presupuesto. | LOG-RF06, LOG-RF07 | LOG-T05 | PENDIENTE |
| 407 | `LOG-T07` Implementar recepcion parcial/total y reversion. | LOG-RF08 | LOG-T06 | PENDIENTE |
| 408 | `LOG-T08` Implementar kardex y proyeccion de stock. | LOG-RF09 | LOG-T07 | PENDIENTE |
| 409 | `LOG-T09` Completar consultas, dashboards API, PDFs y trazabilidad. | LOG-RF01-LOG-RF10 | LOG-T08 | PENDIENTE |
| 410 | `LOG-T10` Completar pruebas de dominio, API y migracion. | LOG-RF01-LOG-RF10 | LOG-T09 | PENDIENTE |

## Fase 5 - Integracion y cierre

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 501 | `INT-T01` Automatizar el escenario E2E completo del MVP1. | Todos los RF | LOG-T10 | PENDIENTE |
| 502 | `INT-T02` Conciliar datos migrados, movimientos y saldos. | RNF-03, RNF-05 | INT-T01 | PENDIENTE |
| 503 | `INT-T03` Ejecutar seguridad, concurrencia, rendimiento y N+1. | RNF-06-RNF-10 | INT-T02 | PENDIENTE |
| 504 | `INT-T04` Construir imagen Docker y validar despliegue desde una base limpia. | RNF-01, RNF-03 | INT-T03 | PENDIENTE |
| 505 | `INT-T05` Ejecutar demostracion de aceptacion y cerrar MVP1. | Todos | INT-T04 | PENDIENTE |

## Fase 6 - Frontend posterior

| Orden | Tarea | Requisitos | Dependencia | Estado |
|---:|---|---|---|---|
| 601 | `FE-T01` Definir shell, autenticacion y navegacion por permisos. | PLT-RF01, PLT-RF02 | INT-T05 | PENDIENTE |
| 602 | `FE-T02` Implementar pantallas de Plataforma y Cuadro. | PLT-RF03-PLT-RF07, CN-RF01-CN-RF08 | FE-T01 | PENDIENTE |
| 603 | `FE-T03` Implementar pantallas de Presupuesto y Logistica. | PRE-RF01-PRE-RF08, LOG-RF01-LOG-RF10 | FE-T02 | PENDIENTE |
| 604 | `FE-T04` Agregar pruebas de componentes y flujo E2E web. | RNF-10 | FE-T03 | PENDIENTE |
