# MVP1 - Arquitectura monolito modular y DDD

## Topologia

La fase actual tendra un solo despliegue backend y una base de datos PostgreSQL administrada:

`Cliente HTTP -> API Spring Boot modular -> PostgreSQL`

El frontend se desarrollara despues de estabilizar el backend. Consumira `/api/v1` y podra desplegarse como contenido estatico o integrarse al artefacto Spring Boot sin cambiar los contratos de dominio. PostgreSQL permanece como infraestructura independiente.

## Modulos y dependencias

| Modulo | Responsabilidad | Puede depender de |
|---|---|---|
| `sharedkernel` | Tipos tecnicos minimos: dinero, referencias, errores e idempotencia | Ninguno |
| `platform` | Identidad, permisos, organizacion, maestros, periodos y auditoria | `sharedkernel` |
| `cuadronecesidades` | Planeamiento anual y consolidacion de necesidades | `platform.api`, `sharedkernel` |
| `presupuesto` | PIA/PIM, disponibilidad y movimientos presupuestales | `platform.api`, `cuadronecesidades.api`, `sharedkernel` |
| `logistica` | Requerimientos, compras, almacen e inventario | APIs publicadas por Plataforma, Cuadro y Presupuesto |

No se permiten ciclos entre modulos. Cada modulo publica contratos en su paquete `api`; los consumidores no importan su `domain`, `infrastructure` ni `repository`.

## Estructura objetivo

```text
com.logistica.demo
|-- sharedkernel
|-- platform
|   |-- domain
|   |-- application
|   |-- infrastructure
|   `-- api
|-- cuadronecesidades
|   |-- domain
|   |-- application
|   |-- infrastructure
|   `-- api
|-- presupuesto
|   |-- domain
|   |-- application
|   |-- infrastructure
|   `-- api
`-- logistica
    |-- domain
    |-- application
    |-- infrastructure
    `-- api
```

## Reglas DDD

- Los controladores traducen HTTP y llaman casos de uso; no contienen reglas de negocio.
- Los agregados validan invariantes y transiciones de estado.
- Los servicios de aplicacion coordinan transacciones y contratos entre modulos.
- Los repositorios persisten agregados completos y pertenecen a un solo modulo.
- Los DTO REST no son entidades JPA ni objetos del dominio.
- Las referencias entre modulos se guardan como identificadores, numero y snapshot descriptivo.
- Toda cantidad monetaria usa `BigDecimal`; nunca `double` o `float`.
- Toda operacion que consuma saldo usa bloqueo concurrente u optimistic locking.
- Los documentos emitidos no se eliminan; se anulan o revierten con historial.

## Agregados principales

| Modulo | Agregados |
|---|---|
| Plataforma | `Usuario`, `Organizacion`, `CatalogItem`, `PeriodoFiscal`, `DocumentSequence` |
| Cuadro | `CuadroNecesidad`, `ConsolidacionCuadro` |
| Presupuesto | `EjercicioPresupuestal`, `LineaPresupuestal`, `ControlPresupuestal` |
| Logistica | `Requerimiento`, `ProcesoCotizacion`, `OrdenCompra`, `RecepcionAlmacen` |

`Stock` sera una proyeccion calculada de movimientos de inventario, no un valor modificable sin kardex.

## Patrones adoptados

- Arquitectura hexagonal mediante puertos de entrada y salida.
- Repository por agregado.
- Application Service por caso de uso.
- State Machine explicita en cada documento.
- Specification para filtros de consulta.
- CQRS ligero: modelos de escritura normalizados y consultas/proyecciones especializadas.
- Domain Events para hechos del negocio.
- Transactional Outbox para efectos posteriores al commit.
- Idempotency Key para comandos repetibles desde web.
- Anti-Corruption Layer para migrar registros de `logistica_demo`.
- ArchUnit para verificar limites de paquetes.

## Contratos internos

- `PlatformCatalogQuery`: companias, centros, fuentes, metas, clasificadores y bienes vigentes.
- `NeedsBalanceQuery`: linea de Cuadro aprobada, dimensiones y saldo de cantidad.
- `TransferNeedsToBudgetUseCase`: transferencia idempotente del consolidado.
- `BudgetAvailabilityQuery`: consulta de disponibilidad por dimensiones y periodo.
- `BudgetControlUseCase`: precomprometer, comprometer y liberar.

Los contratos criticos se invocan en el mismo proceso y participan en la transaccion local. El Outbox no reemplaza la consistencia inmediata del saldo presupuestal.

Las interfaces y DTO exactos estan registrados en `MVP1_CONTRATOS_BACKEND.md`. Su implementacion de persistencia se agregara con los esquemas Flyway sin cambiar estos limites.

## API y seguridad

- Base REST: `/api/v1`.
- Errores: `application/problem+json` con `type`, `title`, `status`, `detail`, `instance`, `code` y `traceId`.
- Paginacion: `content`, `page`, `size`, `totalElements`, `totalPages`.
- Autenticacion: access token corto y refresh token revocable almacenado mediante hash.
- Autorizacion: roles mas alcance por compania, unidad y centro de costo.
- HTTP Basic se eliminara de la configuracion normal.
- Swagger quedara habilitado solo en desarrollo/demo autenticada.
- Acciones sensibles registraran usuario, rol efectivo, IP, fecha y cambio realizado.

## Operacion

- Configuracion exclusivamente mediante variables de entorno.
- Health/readiness mediante Spring Boot Actuator.
- Logs estructurados con `traceId` y sin credenciales ni tokens.
- Indices sobre compania, ejercicio, numero, estado, fechas y claves de relacion.
- Reportes pesados y futuras integraciones se ejecutaran fuera de la transaccion HTTP.
